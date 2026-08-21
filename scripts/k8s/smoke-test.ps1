[CmdletBinding()]
param(
    [string]$Namespace = "opspilot",
    [int]$LocalPort = 18000
)

$ErrorActionPreference = "Stop"
$BaseUrl = "http://127.0.0.1:$LocalPort"
$Timestamp = Get-Date -Format "MMddHHmmss"
$SourceAccount = "62{0}01" -f $Timestamp
$TargetAccount = "62{0}02" -f $Timestamp
$RequestId = "k8s-smoke-$Timestamp"

# 端口转发只在当前 Job 运行；finally 保证脚本成功或失败都会清理后台进程。
$PortForwardJob = Start-Job -ScriptBlock {
    param($Ns, $Port)
    $Mapping = "{0}:8080" -f $Port
    kubectl -n $Ns port-forward service/opspilot-web $Mapping
} -ArgumentList $Namespace, $LocalPort

try {
    Write-Host "[1/5] 等待端口转发和 readiness"
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

    Write-Host "[2/5] 创建付款与收款账户"
    $JsonHeaders = @{ "Content-Type" = "application/json" }
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/accounts" -Headers $JsonHeaders -Body (@{
        accountNo = $SourceAccount
        holderName = "K8s 冒烟付款账户"
        openingBalance = 10000.00
    } | ConvertTo-Json) | Out-Null
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/accounts" -Headers $JsonHeaders -Body (@{
        accountNo = $TargetAccount
        holderName = "K8s 冒烟收款账户"
        openingBalance = 1000.00
    } | ConvertTo-Json) | Out-Null

    Write-Host "[3/5] 执行真实转账"
    $TransferHeaders = @{
        "Content-Type" = "application/json"
        "Idempotency-Key" = $RequestId
    }
    $TransferBody = @{
        sourceAccountNo = $SourceAccount
        targetAccountNo = $TargetAccount
        amount = 250.00
    } | ConvertTo-Json
    $First = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transfers" -Headers $TransferHeaders -Body $TransferBody

    Write-Host "[4/5] 用相同幂等键重放，确认不会重复扣款"
    $Replay = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/transfers" -Headers $TransferHeaders -Body $TransferBody
    if ($First.requestId -ne $Replay.requestId -or $Replay.status -ne "COMPLETED") {
        throw "幂等重放响应不一致。"
    }

    Write-Host "[5/5] 校验双录流水和平衡结果"
    $Audit = Invoke-RestMethod -Uri "$BaseUrl/api/v1/transfers/$RequestId/ledger"
    if (-not $Audit.balanced -or $Audit.entries.Count -ne 2) {
        throw "双录流水不平衡或数量不是 2。"
    }

    Write-Host ""
    Write-Host "Kubernetes 冒烟验收通过："
    Write-Host "  requestId=$RequestId"
    Write-Host "  status=$($First.status)"
    Write-Host "  ledgerEntries=$($Audit.entries.Count)"
    Write-Host "  balanced=$($Audit.balanced)"
}
finally {
    Stop-Job -Job $PortForwardJob -ErrorAction SilentlyContinue
    Remove-Job -Job $PortForwardJob -Force -ErrorAction SilentlyContinue
}
