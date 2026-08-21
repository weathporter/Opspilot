[CmdletBinding()]
param(
    [string]$Namespace = "opspilot",
    # 省略时自动选择当前发布之前最近一次成功版本；也可以显式传入 Helm revision。
    [int]$Revision = 0
)

# 严格模式会把未定义变量变成错误；ErrorActionPreference 负责 PowerShell 自身命令的失败。
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Get-Command helm -ErrorAction SilentlyContinue)) {
    throw "缺少 helm 命令，无法读取发布历史和执行回滚。新安装 Helm 后请重新打开终端。"
}
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "缺少 kubectl 命令，无法验证回滚后的 Deployment。"
}

# Helm 的历史状态中只有当前版本是 deployed，之前成功版本会被标为 superseded。
# 因此候选集必须同时接受这两个状态，但明确排除 failed、pending 和 uninstalled。
$HistoryJson = & helm history opspilot --namespace $Namespace --output json
if ($LASTEXITCODE -ne 0) {
    throw "读取 Helm 发布历史失败。"
}
$History = $HistoryJson | ConvertFrom-Json
$SuccessfulStatuses = @("deployed", "superseded")
$SuccessfulHistory = @(
    $History |
        # Helm 可能把早期 failed revision 在后续升级后标记为 superseded；描述仍保留失败证据，必须一并排除。
        Where-Object { $_.status -in $SuccessfulStatuses -and $_.description -notmatch "failed" } |
        Sort-Object -Property revision -Descending
)
if ($SuccessfulHistory.Count -lt 2) {
    throw "没有可回滚的成功历史版本；至少需要完成两次成功的 Helm 发布。"
}

$CurrentRevision = [int]$SuccessfulHistory[0].revision
if ($Revision -eq 0) {
    $Revision = [int]$SuccessfulHistory[1].revision
}
else {
    # 显式 revision 也必须存在且曾成功部署，避免误回滚到 failed/pending 记录。
    $Target = $SuccessfulHistory | Where-Object { [int]$_.revision -eq $Revision } | Select-Object -First 1
    if (-not $Target) {
        throw "revision $Revision 不存在，或它不是已成功部署的版本。"
    }
}
if ($Revision -eq $CurrentRevision) {
    throw "目标 revision 与当前 revision 相同，不需要回滚。"
}

Write-Host "准备把 opspilot 从 revision $CurrentRevision 回滚到 revision $Revision"
& helm rollback opspilot $Revision --namespace $Namespace --wait --timeout 8m
if ($LASTEXITCODE -ne 0) {
    throw "Helm 回滚失败。"
}

# Helm 返回成功后，再分别等待 API 和 Web Deployment 达到 Available，形成发布门禁。
& kubectl -n $Namespace rollout status deployment/opspilot-api --timeout=180s
if ($LASTEXITCODE -ne 0) {
    throw "API Deployment 在回滚后未按时就绪。"
}
& kubectl -n $Namespace rollout status deployment/opspilot-web --timeout=180s
if ($LASTEXITCODE -ne 0) {
    throw "Web Deployment 在回滚后未按时就绪。"
}

Write-Host "回滚完成，当前 Helm 历史如下："
& helm history opspilot --namespace $Namespace
if ($LASTEXITCODE -ne 0) {
    throw "回滚已完成，但读取最终历史失败，请手工运行 helm history 复核。"
}
