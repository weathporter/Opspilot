<#
.SYNOPSIS
在 Windows + Docker Desktop 上完成构建、Compose 启动和 Nginx 健康门禁。

.DESCRIPTION
默认启动 MySQL、Spring Boot、Nginx；-WithObservability 追加完整监控栈；
-RunTests 先运行 Testcontainers 测试，否则仅打包以缩短本地启动时间。
#>
# WithObservability 控制是否追加完整可观测性栈；RunTests 控制是否先运行真实 MySQL 集成测试。
param(
    [switch]$WithObservability,
    [switch]$RunTests
)

# 未定义变量即报错；任一 PowerShell cmdlet 错误转为终止异常，避免脚本带错继续。
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# PSScriptRoot 是 scripts 目录；切到其父目录保证 Maven 和 Compose 都能找到项目文件。
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# .env 只保存在开发者本机并被 Git 忽略；缺失时立即停止，避免容器以空密码或猜测密码启动。
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot '.env'))) {
    throw 'Missing .env. Copy .env.example to .env and set local passwords before starting.'
}

# 不硬编码 Docker Desktop named pipe，而是读取当前 docker context 的真实端点。
# Testcontainers 和 Maven 子进程也会继承 DOCKER_HOST，从而连接同一个 Linux Engine。
$dockerHostValue = docker context inspect --format '{{.Endpoints.docker.Host}}'
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dockerHostValue)) {
    throw 'Unable to resolve Docker context. Start Docker Desktop first.'
}
$env:DOCKER_HOST = $dockerHostValue.Trim()

if ($RunTests) {
    Write-Host '[1/5] Running MySQL Testcontainers tests'
    # -B 使用批处理模式，-ntp 关闭下载进度条，日志更适合保存和排查。
    mvn -B -ntp test
} else {
    Write-Host '[1/5] Packaging JAR with tests skipped; use -RunTests for a full verification'
    # Dockerfile.runtime 需要主机 target 下已有最新 JAR。
    mvn -B -ntp -DskipTests package
}
if ($LASTEXITCODE -ne 0) {
    # 外部程序的非零退出不会自动受 ErrorActionPreference 管理，因此必须显式检查 LASTEXITCODE。
    throw 'Maven build failed.'
}

Write-Host '[2/5] Building React production assets'
Push-Location (Join-Path $projectRoot 'frontend')
try {
    # 首次运行使用锁文件还原依赖；后续直接复用 node_modules，缩短本地启动时间。
    if (-not (Test-Path 'node_modules')) {
        # 忽略第三方安装脚本，降低依赖安装阶段执行不受信任代码的风险。
        npm ci --ignore-scripts
        if ($LASTEXITCODE -ne 0) {
            throw 'Frontend dependency installation failed.'
        }
    }
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw 'Frontend production build failed.'
    }
} finally {
    Pop-Location
}

# PowerShell 数组保存 Compose 的重复 -f 参数，调用时用 @composeFiles splatting 展开。
$composeFiles = @('-f', 'compose.yml', '-f', 'compose.local.yml')
if ($WithObservability) {
    # 最后一层覆盖 node-exporter 在 Windows Docker Desktop 不支持的 rslave 挂载。
    $composeFiles += @(
        '-f', 'compose.observability.yml',
        '-f', 'compose.desktop-observability.yml'
    )
}

Write-Host '[3/5] Building runtime images and starting containers'
# -d 后台运行；--build 确保新 JAR 被复制进 runtime 镜像。
docker compose @composeFiles up -d --build
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Compose startup failed.'
}

Write-Host '[4/5] Verifying React/Nginx entry health'
$healthy = $false
# 最多尝试 18 次、失败后等待 5 秒，整体约 90 秒，覆盖 MySQL 初始化、Flyway 和 Java 启动。
for ($attempt = 1; $attempt -le 18; $attempt++) {
    try {
        # 访问外部入口而非直接访问 18080，同时验证 Nginx、容器 DNS、应用 readiness 和数据库链路。
        $response = Invoke-RestMethod -Uri 'http://localhost:18000/health' -TimeoutSec 3
        if ($response.status -eq 'UP') {
            $healthy = $true
            break
        }
    } catch {
        # 启动窗口内连接拒绝/502/超时属于可重试状态。
        Start-Sleep -Seconds 5
    }
}
if (-not $healthy) {
    # 失败时先打印容器状态，让用户能看到 unhealthy/exited 服务，再以非零结果终止。
    docker compose @composeFiles ps
    throw 'Nginx entry did not become healthy within 90 seconds.'
}

# readiness 可能受健康组配置变化影响，因此再执行一次带认证的 Redis PING，明确验证共享会话依赖。
$redisPing = docker compose @composeFiles exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
if ($LASTEXITCODE -ne 0 -or $redisPing -notmatch 'PONG') {
    docker compose @composeFiles ps
    throw 'Redis authentication health check failed.'
}

if ($WithObservability) {
    Write-Host '[5/6] Waiting for every observability entry to become ready'

    # 观测容器的“进程已启动”早于“查询接口可用”；逐个等待 2xx，避免过早交付打不开的链接。
    $observabilityEndpoints = [ordered]@{
        Grafana = 'http://localhost:13000/api/health'
        Prometheus = 'http://localhost:19090/-/ready'
        Alertmanager = 'http://localhost:19093/-/ready'
        Loki = 'http://localhost:13100/ready'
        RedisExporter = 'http://localhost:19121/metrics'
    }
    foreach ($entry in $observabilityEndpoints.GetEnumerator()) {
        $endpointReady = $false
        # 最多等待 60 秒；只对启动期连接拒绝或非 2xx 重试，不吞掉最终失败。
        for ($attempt = 1; $attempt -le 20; $attempt++) {
            try {
                $response = Invoke-WebRequest -UseBasicParsing -Uri $entry.Value -TimeoutSec 3
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                    $endpointReady = $true
                    break
                }
            } catch {
                Start-Sleep -Seconds 3
            }
        }
        if (-not $endpointReady) {
            docker compose @composeFiles ps
            throw "$($entry.Key) did not become ready within 60 seconds: $($entry.Value)"
        }
    }
}

Write-Host $(if ($WithObservability) { '[6/6] Container status' } else { '[5/5] Container status' })
docker compose @composeFiles ps
Write-Host 'Nginx entry: http://localhost:18000/health'
if ($WithObservability) {
    # 这些端口均只绑定 127.0.0.1，仅供本机浏览器访问。
    Write-Host 'Grafana: http://localhost:13000'
    Write-Host 'Prometheus: http://localhost:19090'
    Write-Host 'Alertmanager: http://localhost:19093'
    Write-Host 'Redis Exporter metrics: http://localhost:19121/metrics'
}
