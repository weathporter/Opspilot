#requires -Version 7.0
<#
  二节点配置的行为门禁：使用真正的 Helm 渲染结果检查四个 Deployment，
  而不是只搜索 values 文件的文本。此检查不连接或修改集群。
#>
param(
    [string]$HelmExe = $(if ($env:HELM_EXE) { $env:HELM_EXE } else { 'helm' }),
    [string]$ValuesFile = ''
)

$ErrorActionPreference = 'Stop'
$chart = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../deploy/k8s/helm/northledger-microservices')).Path
$profile = if ($ValuesFile) { $ValuesFile } else { Join-Path $chart 'values-two-node-lab.yaml' }
if (-not (Test-Path -LiteralPath $profile)) { throw 'Two-node values profile is missing' }
$profile = (Resolve-Path -LiteralPath $profile).Path

& $HelmExe lint $chart -f $profile
if ($LASTEXITCODE -ne 0) { throw 'Two-node Helm lint failed' }
$render = (& $HelmExe template northledger $chart -n northledger -f $profile) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Two-node Helm template failed' }

# 按文档拆分渲染结果；只检查 Deployment 对象的 spec.replicas，防止
# ConfigMap 中的同名文本误使测试通过。
$documents = [regex]::Split($render, '(?m)^---\s*$')
foreach ($component in @('access', 'ledger', 'operations', 'web')) {
    $name = "northledger-$component"
    $deployment = @($documents | Where-Object {
        $_ -match '(?m)^kind: Deployment\s*$' -and
        $_ -match "(?m)^  name: $([regex]::Escape($name))\s*$"
    })
    if ($deployment.Count -ne 1) { throw "Expected one Deployment $name, found $($deployment.Count)" }
    if ($deployment[0] -notmatch '(?m)^  replicas: 1\s*$') {
        throw "Two-node $name must start with one replica to fit the lab budget"
    }
    if ($component -ne 'web' -and
        $deployment[0] -notmatch 'name: APP_ENV, value: "two-node-lab"') {
        throw "Two-node $name is missing the environment label"
    }
}

if ($render -notmatch 'metadata: \{ name: northledger-default-deny-ingress \}') {
    throw 'Two-node ingress NetworkPolicy must stay enabled'
}
if ($render -notmatch 'kubernetes.io/metadata.name: "monitoring"' -or
    $render -notmatch 'app.kubernetes.io/name: "prometheus"') {
    throw 'Two-node policy must allow the monitored Prometheus Pod to scrape business services'
}
Write-Output 'TWO_NODE_PROFILE_VALIDATION PASS'
