<#
  受保护部署入口：只接受 40 位提交 SHA，Helm 原子升级，浏览器路径冒烟失败则回滚上个修订。
  不创建/删除用户的虚拟机、PV 或数据库；这些基础设施由三节点 Runbook 显式准备。
#>
param(
    [Parameter(Mandatory = $true)][string]$Sha,
    [string]$Namespace = 'northledger',
    [string]$KubeconfigPath = '',
    [string]$ValuesFile = $env:NORTHLEDGER_VALUES_FILE
)

$ErrorActionPreference = 'Stop'
if ($Sha -notmatch '^[0-9a-f]{40}$') { throw 'Image tag must be a full lowercase 40-character commit SHA' }

$temporaryKubeconfig = $null
if ($KubeconfigPath) {
    $env:KUBECONFIG = (Resolve-Path -LiteralPath $KubeconfigPath).Path
} elseif ($env:KUBECONFIG_B64) {
    if (-not $env:RUNNER_TEMP) { throw 'RUNNER_TEMP is required for protected CI deployment' }
    $temporaryKubeconfig = Join-Path $env:RUNNER_TEMP 'northledger-kubeconfig'
    [IO.File]::WriteAllBytes($temporaryKubeconfig,
        [Convert]::FromBase64String($env:KUBECONFIG_B64))
    $env:KUBECONFIG = $temporaryKubeconfig
} else {
    throw 'Provide -KubeconfigPath or protected KUBECONFIG_B64'
}

try {
    $null = Get-Command helm -ErrorAction Stop
    $null = Get-Command kubectl -ErrorAction Stop
    & kubectl get namespace $Namespace -o name | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Namespace $Namespace does not exist" }

    $chart = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../deploy/k8s/helm/northledger-microservices')).Path
    $helmArgs = @('upgrade', '--install', 'northledger', $chart, '-n', $Namespace,
        '--atomic', '--wait', '--timeout', '10m')
    if ($ValuesFile) {
        # 私有 GHCR 拉取 Secret、TLS 等部署差异写在 runner 本机受控文件；不得提交明文到仓库。
        $helmArgs += @('-f', (Resolve-Path -LiteralPath $ValuesFile).Path)
    }
    # SHA 与四个仓库名最后覆盖；runner 的 values 文件不能替换成未经过本次 CI 验证的镜像。
    $helmArgs += @('--set-string', "global.imageTag=$Sha", '--set-string', "global.version=$Sha",
        '--set-string', 'images.access=ghcr.io/weathporter/opspilot-access',
        '--set-string', 'images.ledger=ghcr.io/weathporter/opspilot-ledger',
        '--set-string', 'images.operations=ghcr.io/weathporter/opspilot-operations',
        '--set-string', 'images.web=ghcr.io/weathporter/opspilot-web')
    $previousRevision = 0
    $historyText = (& helm history northledger -n $Namespace -o json 2>$null) -join "`n"
    if ($LASTEXITCODE -eq 0 -and $historyText) {
        $history = @($historyText | ConvertFrom-Json)
        if ($history.Count -gt 0) { $previousRevision = [int]$history[-1].revision }
    }

    & helm @helmArgs
    if ($LASTEXITCODE -ne 0) { throw 'Atomic Helm upgrade failed; Helm handled rollback of this upgrade' }

    try {
        & (Join-Path $PSScriptRoot 'smoke-microservices.ps1') -Namespace $Namespace
    } catch {
        if ($previousRevision -gt 0) {
            Write-Warning "Smoke failed; rolling back NorthLedger to revision $previousRevision"
            & helm rollback northledger $previousRevision -n $Namespace --wait --timeout 10m
            if ($LASTEXITCODE -ne 0) { throw 'Smoke failed and rollback also failed; manual intervention required' }
        } else {
            Write-Warning 'First-install smoke failed; release retained for diagnosis because no prior revision exists'
        }
        throw
    }
    Write-Output "MICROSERVICES_DEPLOY PASS sha=$Sha namespace=$Namespace"
} finally {
    if ($temporaryKubeconfig -and (Test-Path -LiteralPath $temporaryKubeconfig)) {
        Remove-Item -LiteralPath $temporaryKubeconfig -Force
    }
}
