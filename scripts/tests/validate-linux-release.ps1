<#
.SYNOPSIS
静态验证 Linux 原生发布是否同时覆盖 API 与 React 门户。

.DESCRIPTION
该测试不修改主机，也不需要 systemd。它检查构建、发布、回滚和 Nginx 四个边界是否共享
同一版本目录，防止后端已升级但前端仍停留在旧版本。
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Assert-Contains {
    param([string]$Path, [string]$Pattern, [string]$Message)
    $content = Get-Content -LiteralPath (Join-Path $projectRoot $Path) -Raw
    if ($content -notmatch $Pattern) {
        throw "$Message ($Path)"
    }
}

# 构建必须产出前端 dist，发布脚本必须接收并复制它。
Assert-Contains 'scripts/build.sh' 'npm run build' 'build.sh must build React production assets'
Assert-Contains 'scripts/deploy.sh' '--web-dist' 'deploy.sh must accept an explicit frontend dist directory'
Assert-Contains 'scripts/deploy.sh' 'RELEASE_DIR.*web' 'deploy.sh must store frontend assets inside the immutable release'

# Nginx 必须静态托管 React，并只把 API/健康请求代理给 Spring Boot。
Assert-Contains 'deploy/linux/nginx/opspilot.conf' 'root /opt/opspilot/current/web;' 'Nginx must serve the current React release'
Assert-Contains 'deploy/linux/nginx/opspilot.conf' 'location /api/' 'Nginx must proxy API requests separately'
Assert-Contains 'deploy/linux/nginx/opspilot.conf' 'try_files \$uri \$uri/ /index.html;' 'Nginx must support React BrowserRouter fallback'

# 没有 index.html 的历史目录不能成为回滚目标，否则会出现 API 正常但页面空白。
Assert-Contains 'scripts/rollback.sh' 'web/index.html' 'rollback must reject releases without frontend assets'

# Linux 环境必须注入 Redis 地址/密码，巡检和诊断包也要覆盖协议连通与关键运行指标。
Assert-Contains 'deploy/linux/env/opspilot.env.example' 'REDIS_PASSWORD=CHANGE_ME' 'Linux env template must require a Redis secret'
Assert-Contains 'deploy/linux/env/redis.env.example' 'REDIS_PASSWORD=CHANGE_ME' 'Redis container must have a dedicated minimal secret file'
Assert-Contains 'docs/runbooks/linux-deployment.md' '--env-file /etc/opspilot/redis.env' 'Linux Redis container must not receive the full application environment'
Assert-Contains 'scripts/init-server.sh' 'dnf install[^\r\n]*\bredis\b' 'fresh Rocky Linux hosts must install redis-cli for protocol diagnostics'
Assert-Contains 'scripts/inspection.sh' 'redis-cli.*--no-auth-warning' 'inspection must perform an authenticated Redis check'
Assert-Contains 'scripts/inspection.sh' 'read_env_value /etc/opspilot/opspilot.env REDIS_PASSWORD' 'inspection must parse only the Redis password without sourcing the EnvironmentFile'
Assert-Contains 'scripts/collect-diagnostics.sh' 'INFO server memory stats' 'diagnostics must capture bounded Redis INFO sections'

Write-Output 'Linux release contract passed.'
