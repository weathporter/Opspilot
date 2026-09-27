<#
  从 Web/Access 的同源入口走真实登录、会话、CSRF、Ledger 查询、Operations 看板和对账。
  只在 Operations 追加一次对账记录，不创建或修改资金账户、订单与流水。
#>
param(
    [string]$Namespace = 'northledger',
    [int]$LocalPort = 18199
)

$ErrorActionPreference = 'Stop'
if (-not $env:SMOKE_USERNAME -or -not $env:SMOKE_PASSWORD) {
    throw 'SMOKE_USERNAME and SMOKE_PASSWORD must be injected by the protected environment'
}

$forward = $null
$base = "http://127.0.0.1:$LocalPort"
try {
    # 仅监听 runner 的 loopback；不把集群服务临时公开到宿主机局域网。
    $forward = Start-Process -FilePath 'kubectl' -ArgumentList @(
        '-n', $Namespace, 'port-forward', 'svc/northledger-web',
        "${LocalPort}:8080", '--address', '127.0.0.1'
    ) -WindowStyle Hidden -PassThru

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($forward.HasExited) { throw 'kubectl port-forward exited before Web became ready' }
        try {
            $health = Invoke-RestMethod -Uri "$base/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch {
            # Pod 切流、port-forward 建连期间短暂拒绝连接属于预期；最多等待 30 秒。
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'Web/Access readiness did not become UP within 30 seconds' }

    $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $anonymous = Invoke-RestMethod -Uri "$base/api/v1/auth/session" -WebSession $session -TimeoutSec 8
    if ($anonymous.authenticated) { throw 'Anonymous session unexpectedly authenticated' }

    $unauthorized = Invoke-WebRequest -Uri "$base/api/v1/accounts" -WebSession $session `
        -SkipHttpErrorCheck -TimeoutSec 8
    if ($unauthorized.StatusCode -ne 401) { throw 'Unauthenticated accounts request did not return 401' }

    $csrfCookie = $session.Cookies.GetCookies($base) | Where-Object Name -eq 'XSRF-TOKEN' | Select-Object -First 1
    if (-not $csrfCookie) { throw 'Access did not issue XSRF-TOKEN' }
    $csrf = [uri]::UnescapeDataString($csrfCookie.Value)
    $null = Invoke-RestMethod -Uri "$base/api/v1/auth/session" -Method Post -WebSession $session `
        -ContentType 'application/x-www-form-urlencoded' `
        -Headers @{ 'X-XSRF-TOKEN' = $csrf } `
        -Body @{ username = $env:SMOKE_USERNAME; password = $env:SMOKE_PASSWORD } -TimeoutSec 8

    # 登录会轮换 Session/CSRF，重新读取一次再做状态修改请求。
    $authenticated = Invoke-RestMethod -Uri "$base/api/v1/auth/session" -WebSession $session -TimeoutSec 8
    if (-not $authenticated.authenticated) { throw 'Access login did not create an authenticated session' }
    $csrfCookie = $session.Cookies.GetCookies($base) | Where-Object Name -eq 'XSRF-TOKEN' | Select-Object -First 1
    if (-not $csrfCookie) { throw 'Access did not refresh XSRF-TOKEN after login' }
    $csrf = [uri]::UnescapeDataString($csrfCookie.Value)

    $accounts = Invoke-RestMethod -Uri "$base/api/v1/accounts?limit=1" -WebSession $session -TimeoutSec 10
    $summary = Invoke-RestMethod -Uri "$base/api/v1/operations/summary" -WebSession $session -TimeoutSec 10
    if ($null -eq $summary.accountCount -or $null -eq $summary.environment) {
        throw 'Operations summary contract is incomplete'
    }

    $csrfBlocked = Invoke-WebRequest -Uri "$base/api/v1/operations/reconciliations?limit=1" `
        -Method Post -WebSession $session -SkipHttpErrorCheck -TimeoutSec 8
    if ($csrfBlocked.StatusCode -ne 403) { throw 'Reconciliation POST without CSRF did not return 403' }

    $run = Invoke-RestMethod -Uri "$base/api/v1/operations/reconciliations?limit=20" `
        -Method Post -WebSession $session -Headers @{ 'X-XSRF-TOKEN' = $csrf } -TimeoutSec 15
    if ($null -eq $run.id -or $null -eq $run.checkedCount) {
        throw 'Operations reconciliation did not return a persisted run'
    }
    $detail = Invoke-RestMethod -Uri "$base/api/v1/operations/reconciliations/$($run.id)" `
        -WebSession $session -TimeoutSec 8
    if ($detail.id -ne $run.id) { throw 'Persisted reconciliation detail cannot be read back' }

    Write-Output "MICROSERVICES_SMOKE PASS checked=$($run.checkedCount) discrepancies=$($run.discrepancyCount)"
} finally {
    if ($forward -and -not $forward.HasExited) {
        Stop-Process -Id $forward.Id -Force -ErrorAction SilentlyContinue
    }
}
