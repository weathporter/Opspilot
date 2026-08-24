<#
.SYNOPSIS
在 Docker Desktop 本地环境执行有界 Redis 停止与恢复演练。

.DESCRIPTION
脚本只操作当前 OpsPilot Compose 项目的 redis 服务，最长停 120 秒，并在 finally 中确保恢复。
必须显式传入 -Confirm LAB_ONLY，防止误把演练动作当作日常运维命令。
#>
param(
    [ValidateRange(5, 120)]
    [int]$DurationSeconds = 20,
    [string]$Confirm
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Confirm -ne 'LAB_ONLY') {
    throw 'Redis fault drill requires -Confirm LAB_ONLY and must only run in the local lab.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
# 先解析 redis 容器 ID，确保目标严格属于当前 Compose 项目；空值时拒绝执行。
# 故意展开为显式参数：项目面向 Windows PowerShell 5.1，原生命令的数组 splatting 在不同版本间行为不一致。
# 显式转成字符串可保证“命令无输出”时变量仍为已定义的空字符串，StrictMode 下也能安全检查。
$script:redisContainerId = ''
$script:redisContainerId = [string](& docker compose -f compose.yml -f compose.local.yml -f compose.observability.yml -f compose.desktop-observability.yml ps -q redis)
$containerLookupSucceeded = $?
if (-not $containerLookupSucceeded -or [string]::IsNullOrWhiteSpace($script:redisContainerId)) {
    throw 'The current OpsPilot Compose project has no running redis container.'
}

try {
    Write-Host "Stopping Redis for $DurationSeconds seconds. Observe application logs, Prometheus and Alertmanager."
    # 后续动作始终使用上面解析并验证过的精确容器 ID，避免恢复阶段再次依赖 Compose 状态判断。
    # Docker 29 已弃用 --time，并会把警告写入 stderr；在严格错误策略下改用等价的新参数。
    & docker stop --timeout 10 $script:redisContainerId
    $stopSucceeded = $?
    if (-not $stopSucceeded) {
        throw 'Failed to stop the scoped Redis container.'
    }
    Start-Sleep -Seconds $DurationSeconds
} finally {
    # 即使用户中断等待或前面抛错，也恢复当前项目的 Redis，避免把实验故障遗留到下一次使用。
    & docker start $script:redisContainerId
    if (-not $?) {
        throw 'Failed to restart the scoped Redis container; follow docs/runbooks/redis-degradation.md.'
    }
}

for ($attempt = 1; $attempt -le 20; $attempt++) {
    # `up -d` 返回后容器可能仍处于 starting；此时 compose exec 会向 stderr 写“not running”。
    # 这里把该预期竞争窗口降为重试信号，其他阶段仍保持全局 Stop，避免真正错误被静默忽略。
    $script:ping = ''
    $pingSucceeded = $false
    try {
        $ErrorActionPreference = 'Continue'
        $script:ping = [string](& docker exec -i $script:redisContainerId sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping' 2>$null)
        $pingSucceeded = $?
    } finally {
        $ErrorActionPreference = 'Stop'
    }
    if ($pingSucceeded -and $script:ping -match 'PONG') {
        Write-Output 'Redis recovered. Verify application readiness, login and alert resolution.'
        exit 0
    }
    Start-Sleep -Seconds 3
}

throw 'Redis did not recover within 60 seconds; follow docs/runbooks/redis-degradation.md.'
