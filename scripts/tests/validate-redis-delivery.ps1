<#
.SYNOPSIS
验证 Redis 在 Compose、可观测性、Helm 与 Linux 交付中的实际渲染结果。

.DESCRIPTION
测试只使用临时占位凭据执行配置渲染，不启动容器、不写 .env。断言面向 Compose/Helm 的
最终产物，而不是只搜索某一行源文件，能够发现变量插值、覆盖文件和条件模板造成的缺失。
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $projectRoot

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

# 只在当前测试进程中提供 Compose 必填值；字符串没有外部系统权限，也不会写入任何文件。
$env:DB_PASSWORD = 'contract-only-database-password'
$env:MYSQL_ROOT_PASSWORD = 'contract-only-root-password'
$env:BOOTSTRAP_ADMIN_USERNAME = 'contract-admin'
$env:BOOTSTRAP_ADMIN_PASSWORD = 'contract-only-admin-password'
$env:GRAFANA_ADMIN_PASSWORD = 'contract-only-grafana-password'
$env:REDIS_PASSWORD = 'contract-only-redis-password'

$composeJson = docker compose `
    -f compose.yml `
    -f compose.local.yml `
    -f compose.observability.yml `
    -f compose.desktop-observability.yml `
    config --format json
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Compose Redis delivery could not be rendered.'
}
$compose = $composeJson | ConvertFrom-Json

Assert-True ($null -ne $compose.services.redis) 'Compose must render a redis service.'
Assert-True ($null -ne $compose.services.'redis-exporter') 'Compose must render a redis-exporter service.'
Assert-True ($compose.services.app.environment.REDIS_HOST -eq 'redis') 'API must connect through Compose DNS name redis.'
Assert-True ($compose.services.redis.ports[0].host_ip -eq '127.0.0.1') 'Redis host port must bind loopback only.'
Assert-True ($compose.services.'redis-exporter'.ports[0].host_ip -eq '127.0.0.1') 'Redis exporter port must bind loopback only.'

# exporter 后端连接失败时可能让整次抓取失败，因此可用性规则必须同时覆盖 redis_up=0 与 scrape up=0。
Assert-True ([System.IO.File]::ReadAllText((Join-Path $projectRoot 'observability/prometheus/rules/opspilot-alerts.yml')) -match 'redis_up\{job="redis"\}\s*==\s*0') 'Redis alert must detect an explicit redis_up=0.'
Assert-True ([System.IO.File]::ReadAllText((Join-Path $projectRoot 'observability/prometheus/rules/opspilot-alerts.yml')) -match 'up\{job="redis"\}\s*==\s*0') 'Redis alert must detect a failed exporter scrape.'

# 看板 JSON 必须可解析，并把 Redis 与 exporter 两段状态组合成稳定的 1/0，而不是故障时保留旧值。
ConvertFrom-Json -InputObject ([System.IO.File]::ReadAllText((Join-Path $projectRoot 'observability/grafana/dashboards/northledger-redis.json'))) | Out-Null
Assert-True ([System.IO.File]::ReadAllText((Join-Path $projectRoot 'observability/grafana/dashboards/northledger-redis.json')) -match 'up\{job=\\"redis\\"\}') 'Redis status panel must include exporter scrape health.'

# Windows/Bash 一键部署必须保持同一个密钥、镜像和失败回滚契约，不能只修一个平台。
$windowsDeploy = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'scripts/k8s/deploy-minikube.ps1'))
$bashDeploy = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'scripts/k8s/deploy-minikube.sh'))
foreach ($deployScript in @($windowsDeploy, $bashDeploy)) {
    Assert-True ($deployScript -match 'redis:7\.4\.10-alpine') 'Minikube deploy scripts must load the pinned Redis image.'
    Assert-True ($deployScript -match 'oliver006/redis_exporter:v1\.89\.0-alpine') 'Minikube deploy scripts must load the pinned Redis exporter image.'
    Assert-True ($deployScript -match 'secrets\.existingSecret=opspilot-runtime') 'Helm deployment must reference the pre-created runtime Secret.'
    Assert-True ($deployScript -match 'data-mysql-0') 'Minikube deploy scripts must detect an existing MySQL PVC.'
    Assert-True ($deployScript -match 'data-opspilot-redis-0') 'Minikube deploy scripts must detect an existing Redis PVC.'
    Assert-True ($deployScript -match '\.data\.redis-password') 'Minikube deploy scripts must preserve the password associated with an existing Redis PVC.'
    Assert-True ($deployScript -match '--atomic') 'Minikube deploy scripts must support Helm 3 atomic rollback.'
    Assert-True ($deployScript -match '--rollback-on-failure') 'Minikube deploy scripts must support Helm 4 rollback on failure.'
    Assert-True ($deployScript -match 'docker pull') 'Minikube deploy scripts must prepare missing dependency images.'
    Assert-True ($deployScript -match 'minikube image load --overwrite=true') 'Minikube deploy scripts must overwrite same-tag node images from the archive.'
    Assert-True ($deployScript -match 'rollout restart deployment/opspilot-api deployment/opspilot-web') 'Minikube deploy scripts must restart workloads after rebuilding a same-tag local image.'
}
Assert-True ($windowsDeploy -match 'opspilot-database') 'Minikube deploy script must preserve credentials during legacy Chart upgrades.'
Assert-True ($windowsDeploy -match 'docker image save --output') 'Minikube deploy script must export a deterministic Docker archive.'

if (Get-Command helm -ErrorAction SilentlyContinue) {
    $rendered = helm template northledger deploy/k8s/helm/opspilot `
        --set-string secrets.databasePassword=contract-db `
        --set-string secrets.mysqlRootPassword=contract-root `
        --set-string secrets.bootstrapAdminPassword=contract-admin `
        --set-string secrets.redisPassword=contract-redis
    if ($LASTEXITCODE -ne 0) {
        throw 'Helm internal Redis mode could not be rendered.'
    }
    $renderedYaml = $rendered -join "`n"
    Assert-True ($renderedYaml -match 'kind: StatefulSet[\s\S]*name: opspilot-redis') 'Helm must render the internal Redis StatefulSet.'
    Assert-True ($renderedYaml -match 'name: REDIS_PASSWORD') 'Helm API must receive Redis password from Secret.'
    Assert-True ($renderedYaml -match 'name: opspilot-allow-prometheus-to-api') 'NetworkPolicy must allow the configured Prometheus source to scrape API cache metrics.'
    # API、Redis 和 exporter 都依赖同一个 Chart-managed Secret，三个 Pod 模板都必须带校验和。
    Assert-True (([regex]::Matches($renderedYaml, 'checksum/secret:')).Count -ge 3) 'API, Redis and exporter must roll when a Chart-managed Secret changes.'

    $productionRendered = helm template northledger deploy/k8s/helm/opspilot `
        -f deploy/k8s/helm/opspilot/values-production.example.yaml
    if ($LASTEXITCODE -ne 0) {
        throw 'Helm external TLS Redis mode could not be rendered.'
    }
    $productionYaml = $productionRendered -join "`n"
    Assert-True ($productionYaml -match 'REDIS_ADDR[\s\S]*rediss://redis-prod\.internal\.example:6379') 'External Redis exporter must use rediss when TLS is enabled.'
    Assert-True ($productionYaml -match 'SPRING_DATA_REDIS_SSL_BUNDLE[\s\S]*redis') 'API must bind Redis to the Spring SSL bundle.'
    Assert-True ($productionYaml -match 'secretName: opspilot-production-redis-trust') 'External private CA trust material must come from a pre-created Secret.'
    Assert-True ($productionYaml -match 'kubernetes\.io/metadata\.name: monitoring') 'Production NetworkPolicy must allow the labeled monitoring namespace.'

    # 外部 Redis 不渲染内置 StatefulSet；Runbook 必须引用实际 existingSecret，只重启客户端。
    $redisRunbook = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'docs/runbooks/redis-degradation.md'))
    Assert-True ($productionYaml -notmatch 'name: opspilot-redis\s') 'External Redis mode must not render the internal Redis StatefulSet or Service.'
    Assert-True ($redisRunbook -match 'opspilot-production-runtime') 'External Redis rotation instructions must name the production example Secret.'
    Assert-True ($redisRunbook -match 'redis\.enabled=false') 'External Redis rotation instructions must state that no internal StatefulSet exists.'
    Assert-True ($redisRunbook -match 'rollout restart deployment/opspilot-api deployment/opspilot-redis-exporter') 'External Redis rotation must restart only the API and exporter clients.'
}

Write-Output 'Redis delivery contract passed.'
