<#
  静态交付门禁：必须渲染出三个独立后端和 Web，且公开入口只指向 Web。
  该脚本不访问集群，也不能替代用户在三节点上的真实运行验收。
#>
param(
    [string]$HelmExe = $(if ($env:HELM_EXE) { $env:HELM_EXE } else { 'helm' })
)

$ErrorActionPreference = 'Stop'
$chart = Join-Path $PSScriptRoot '../../deploy/k8s/helm/northledger-microservices'
$chart = (Resolve-Path -LiteralPath $chart).Path
$sha = '1234567890abcdef1234567890abcdef12345678'

& $HelmExe lint $chart
if ($LASTEXITCODE -ne 0) { throw 'Helm lint failed' }

$render = (& $HelmExe template northledger $chart --namespace northledger `
    --set-string "global.imageTag=$sha" --set-string "global.version=$sha") -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Helm template failed' }

# 不要求 YAML 文档顺序，只验证每个独立工作负载和镜像的存在性。
foreach ($component in @('access', 'ledger', 'operations', 'web')) {
    $deployment = "name: northledger-$component"
    $image = "ghcr.io/weathporter/opspilot-${component}:$sha"
    if (-not $render.Contains($deployment)) { throw "Missing deployment for $component" }
    if (-not $render.Contains($image)) { throw "Missing immutable image for $component" }
}

foreach ($stateful in @('mysql', 'redis')) {
    if (-not $render.Contains("name: northledger-$stateful")) {
        throw "Missing stateful dependency $stateful"
    }
}

if ($render.Contains('opspilot-backend')) { throw 'Legacy single backend remains in microservices release' }
if (-not $render.Contains('name: northledger-web')) { throw 'Web ingress target is missing' }
foreach ($key in @('access-db-password', 'ledger-db-password', 'operations-db-password',
                   'internal-access-token', 'internal-operations-token', 'internal-operations-read-token')) {
    if (-not $render.Contains("key: $key")) { throw "Missing Secret reference $key" }
}

$ingressCount = [regex]::Matches($render, '(?m)^kind: Ingress\s*$').Count
if ($ingressCount -ne 1) { throw "Expected one public Ingress, found $ingressCount" }

# 私有 GHCR 场景必须让四个镜像 Pod 都继承同一个预建拉取 Secret。
$privateRender = (& $HelmExe template northledger $chart --namespace northledger `
    --set-string 'global.imagePullSecrets[0].name=ghcr-pull') -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Helm private registry template failed' }
$pullSecretCount = [regex]::Matches($privateRender, '(?m)^\s+- name: ghcr-pull\s*$').Count
if ($pullSecretCount -ne 4) {
    throw "Expected imagePullSecrets on four Deployments, found $pullSecretCount"
}

Write-Output 'MICROSERVICES_CHART_VALIDATION PASS'
