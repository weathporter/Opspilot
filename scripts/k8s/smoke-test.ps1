[CmdletBinding()]
param(
    [string]$Namespace = "opspilot",
    [int]$LocalPort = 18000
)

$ErrorActionPreference = "Stop"
$WebSession = $null
$BaseUrl = "http://127.0.0.1:$LocalPort"
$Timestamp = Get-Date -Format "MMddHHmmss"
$SourceAccount = "62{0}01" -f $Timestamp
$TargetAccount = "62{0}02" -f $Timestamp
$InvalidationAccount = "62{0}03" -f $Timestamp
$RequestId = "k8s-smoke-$Timestamp"

function Get-CsrfToken {
    param([Parameter(Mandatory)]$Session)
    $Cookie = $Session.Cookies.GetCookies($BaseUrl) |
        Where-Object { $_.Name -eq "XSRF-TOKEN" } |
        Select-Object -First 1
    if ($null -eq $Cookie) {
        throw "认证响应没有设置 XSRF-TOKEN Cookie。"
    }
    return [Uri]::UnescapeDataString($Cookie.Value)
}

# 从 Kubernetes Secret 读取集群当前引导账号，只保存在脚本内存中，不输出、不写文件、不进入命令行参数。
$EncodedAdminUsername = [string](& kubectl -n $Namespace get secret opspilot-runtime "-o=jsonpath={.data.bootstrap-admin-username}")
$EncodedAdminPassword = [string](& kubectl -n $Namespace get secret opspilot-runtime "-o=jsonpath={.data.bootstrap-admin-password}")
if ([string]::IsNullOrWhiteSpace($EncodedAdminUsername) -or [string]::IsNullOrWhiteSpace($EncodedAdminPassword)) {
    throw "opspilot-runtime Secret 缺少冒烟测试所需的管理员凭据。"
}
$AdminUsername = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedAdminUsername))
$AdminPassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedAdminPassword))

# 端口转发只在当前 Job 运行；finally 保证脚本成功或失败都会清理后台进程。
$PortForwardJob = Start-Job -ScriptBlock {
    param($Ns, $Port)
    $Mapping = "{0}:8080" -f $Port
    kubectl -n $Ns port-forward service/opspilot-web $Mapping
} -ArgumentList $Namespace, $LocalPort

try {
    Write-Host "[1/8] 等待端口转发和 readiness"
    $Ready = $false
    for ($Attempt = 1; $Attempt -le 30; $Attempt++) {
        try {
            $Health = Invoke-RestMethod -Uri "$BaseUrl/health" -TimeoutSec 2
            if ($Health.status -eq "UP") {
                $Ready = $true
                break
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $Ready) {
        throw "在 60 秒内未通过 $BaseUrl/health readiness 检查。"
    }

    Write-Host "[2/8] 建立带 CSRF 保护的管理员会话"
    $WebSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $Anonymous = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/session" -WebSession $WebSession -TimeoutSec 10
    if ($Anonymous.authenticated) {
        throw "全新 WebSession 不应处于已认证状态。"
    }
    $CsrfToken = Get-CsrfToken -Session $WebSession
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/session" -WebSession $WebSession `
        -Headers @{ "X-XSRF-TOKEN" = $CsrfToken } `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{ username = $AdminUsername; password = $AdminPassword } | Out-Null
    $Authenticated = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/session" -WebSession $WebSession -TimeoutSec 10
    if (-not $Authenticated.authenticated -or $Authenticated.roles -notcontains "ADMIN") {
        throw "管理员会话未建立，无法执行受保护的业务冒烟。"
    }

    # 登录成功会轮换 Session/CSRF；后续所有修改请求都必须使用新 Token 和同一 Cookie 会话。
    $CsrfToken = Get-CsrfToken -Session $WebSession
    $JsonHeaders = @{ "Content-Type" = "application/json"; "X-XSRF-TOKEN" = $CsrfToken }

    Write-Host "[3/8] 创建付款与收款账户"
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/accounts" -WebSession $WebSession -Headers $JsonHeaders -Body (@{
        accountNo = $SourceAccount
        holderName = "K8s 冒烟付款账户"
        openingBalance = 10000.00
    } | ConvertTo-Json) | Out-Null
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/accounts" -WebSession $WebSession -Headers $JsonHeaders -Body (@{
        accountNo = $TargetAccount
        holderName = "K8s 冒烟收款账户"
        openingBalance = 1000.00
    } | ConvertTo-Json) | Out-Null

    Write-Host "[4/8] 执行真实转账"
    $TransferHeaders = @{
        "Content-Type" = "application/json"
        "Idempotency-Key" = $RequestId
        "X-XSRF-TOKEN" = $CsrfToken
    }
    $TransferBody = @{
        sourceAccountNo = $SourceAccount
        targetAccountNo = $TargetAccount
        amount = 250.00
    } | ConvertTo-Json
    $First = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transfers" -WebSession $WebSession -Headers $TransferHeaders -Body $TransferBody

    Write-Host "[5/8] 用相同幂等键重放，确认不会重复扣款"
    $Replay = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transfers" -WebSession $WebSession -Headers $TransferHeaders -Body $TransferBody
    if ($First.requestId -ne $Replay.requestId -or $Replay.status -ne "COMPLETED") {
        throw "幂等重放响应不一致。"
    }

    Write-Host "[6/8] 校验双录流水和平衡结果"
    $Audit = Invoke-RestMethod -Uri "$BaseUrl/api/v1/transfers/$RequestId/ledger" -WebSession $WebSession
    if (-not $Audit.balanced -or $Audit.entries.Count -ne 2) {
        throw "双录流水不平衡或数量不是 2。"
    }

    Write-Host "[7/8] 验证 Redis 会话与总览缓存键"
    $SessionKeyCount = [int]([string](& kubectl -n $Namespace exec statefulset/opspilot-redis -- sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --scan --pattern "northledger:session:*" | wc -l')).Trim()
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/operations/summary" -WebSession $WebSession | Out-Null
    $CacheTtl = [int]([string](& kubectl -n $Namespace exec statefulset/opspilot-redis -- sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli TTL "northledger:cache:operations-summary::current"')).Trim()
    if ($SessionKeyCount -lt 1 -or $CacheTtl -le 0) {
        throw "Redis 会话或总览缓存没有形成有效键/TTL。"
    }

    Write-Host "[8/8] 验证数据库提交后总览缓存立即失效并可重新生成"
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/accounts" -WebSession $WebSession -Headers $JsonHeaders -Body (@{
        accountNo = $InvalidationAccount
        holderName = "K8s 缓存失效验收账户"
        openingBalance = 0.00
    } | ConvertTo-Json) | Out-Null
    # Redis TTL=-2 表示键不存在；HTTP 成功返回前必须已经完成数据库提交和 AFTER_COMMIT 失效事件。
    $TtlAfterCommit = [int]([string](& kubectl -n $Namespace exec statefulset/opspilot-redis -- sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli TTL "northledger:cache:operations-summary::current"')).Trim()
    if ($TtlAfterCommit -ne -2) {
        throw "业务写入提交后总览缓存仍存在，实际 TTL=$TtlAfterCommit。"
    }
    Invoke-RestMethod -Uri "$BaseUrl/api/v1/operations/summary" -WebSession $WebSession | Out-Null
    $RegeneratedCacheTtl = [int]([string](& kubectl -n $Namespace exec statefulset/opspilot-redis -- sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli TTL "northledger:cache:operations-summary::current"')).Trim()
    if ($RegeneratedCacheTtl -le 0) {
        throw "失效后的总览读取没有重新生成有限 TTL 缓存。"
    }

    Write-Host ""
    Write-Host "Kubernetes 冒烟验收通过："
    Write-Host "  requestId=$RequestId"
    Write-Host "  status=$($First.status)"
    Write-Host "  ledgerEntries=$($Audit.entries.Count)"
    Write-Host "  balanced=$($Audit.balanced)"
    Write-Host "  redisSessionKeys=$SessionKeyCount"
    Write-Host "  dashboardCacheTtl=$RegeneratedCacheTtl"
    Write-Host "  cacheEvictedAfterCommit=True"
}
finally {
    if ($null -ne $WebSession) {
        try {
            $LogoutCsrf = Get-CsrfToken -Session $WebSession
            Invoke-WebRequest -UseBasicParsing -Method Delete -Uri "$BaseUrl/api/v1/auth/session" `
                -WebSession $WebSession -Headers @{ "X-XSRF-TOKEN" = $LogoutCsrf } -TimeoutSec 5 | Out-Null
        }
        catch {
            # 冒烟主体结果优先；端口转发或会话已结束时，注销失败不覆盖原始异常。
        }
    }
    Stop-Job -Job $PortForwardJob -ErrorAction SilentlyContinue
    Remove-Job -Job $PortForwardJob -Force -ErrorAction SilentlyContinue
}
