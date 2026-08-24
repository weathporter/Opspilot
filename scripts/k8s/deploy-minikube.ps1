[CmdletBinding()]
param(
    [string]$Version = "0.3.0",
    [string]$Namespace = "opspilot",
    [switch]$SkipBuild,
    # 镜像网络受限时可跳过 Minikube 插件；核心业务仍可通过 Service 端口转发验收。
    [switch]$SkipAddons,
    # 集群存在失效 Ingress Webhook 时，本次 Release 不创建 Ingress 对象，避免修改集群级安全配置。
    [switch]$DisableIngress
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ChartPath = Join-Path $ProjectRoot "deploy\k8s\helm\opspilot"
$LocalEnvPath = Join-Path $ProjectRoot ".env"

function Assert-Command {
    param([Parameter(Mandatory)][string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少命令 $Name。请先按 deploy/k8s/README.md 安装，再重新执行。"
    }
}

function Assert-NativeSuccess {
    param([Parameter(Mandatory)][string]$Message)
    # PowerShell 5.1 不会因外部程序非零退出自动抛错，因此每一步都必须显式检查。
    if ($LASTEXITCODE -ne 0) {
        throw $Message
    }
}

function Get-RequiredDeploymentValue {
    param([Parameter(Mandatory)][string]$Name)

    # CI/运维平台优先从进程环境注入；本机学习环境可以复用已被 .gitignore 排除的 .env。
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value) -and (Test-Path $LocalEnvPath)) {
        foreach ($line in [System.IO.File]::ReadAllLines($LocalEnvPath)) {
            if ($line.StartsWith("$Name=")) {
                $value = $line.Substring($Name.Length + 1)
                break
            }
        }
    }
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "缺少 $Name。请在当前进程环境或已忽略的 .env 中设置后再部署。"
    }
    return $value
}

function Import-LocalImageToMinikube {
    param([Parameter(Mandatory)][string]$Image)

    # Docker Desktop 的 BuildKit 可能生成带 attestations 的 manifest list；同标签 daemon 直载在部分
    # Minikube/Docker 组合中不会替换节点旧镜像。先导出标准 archive，再加载并删除临时文件，行为可验证。
    $TempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    $TransferDirectory = Join-Path $TempRoot ("northledger-image-transfer-" + [Guid]::NewGuid().ToString("N"))
    $ResolvedParent = [System.IO.Path]::GetFullPath((Split-Path -Parent $TransferDirectory))
    if ($ResolvedParent.TrimEnd("\") -ne $TempRoot.TrimEnd("\")) {
        throw "镜像传输临时目录不在系统临时目录内，拒绝继续。"
    }

    New-Item -ItemType Directory -Path $TransferDirectory | Out-Null
    $Archive = Join-Path $TransferDirectory "image.tar"
    try {
        & docker image save --output $Archive $Image
        Assert-NativeSuccess "无法从 Docker Desktop 导出镜像 $Image。"
        & minikube image load --overwrite=true $Archive
        Assert-NativeSuccess "无法把镜像 $Image 加载到 Minikube。"
    }
    finally {
        # 目标都是本函数创建并验证过的单个路径，不做递归删除，不接收外部通配符。
        if (Test-Path -LiteralPath $Archive) { Remove-Item -LiteralPath $Archive -Force }
        if (Test-Path -LiteralPath $TransferDirectory) { Remove-Item -LiteralPath $TransferDirectory -Force }
    }
}

@("docker", "minikube", "kubectl", "helm", "mvn", "npm") |
    ForEach-Object { Assert-Command $_ }

Write-Host "[1/11] 检查 Docker Desktop 引擎"
& docker info | Out-Null
Assert-NativeSuccess "Docker Desktop 不可用，请先启动引擎。"

Write-Host "[2/11] 确保 Minikube 集群运行"
$MinikubeState = & minikube status --format "{{.Host}}" 2>$null
if ($LASTEXITCODE -ne 0 -or $MinikubeState -ne "Running") {
    # 已存在的 profile 会保留原资源配置；脚本不会擅自删除用户集群。
    & minikube start --driver=docker --cpus=4 --memory=6144
    Assert-NativeSuccess "Minikube 启动失败。"
}

Write-Host "[3/11] 创建命名空间并安全注入运行密钥"
$DatabasePassword = Get-RequiredDeploymentValue "DB_PASSWORD"
$MysqlRootPassword = Get-RequiredDeploymentValue "MYSQL_ROOT_PASSWORD"
$RedisPassword = Get-RequiredDeploymentValue "REDIS_PASSWORD"
$BootstrapAdminUsername = Get-RequiredDeploymentValue "BOOTSTRAP_ADMIN_USERNAME"
$BootstrapAdminPassword = Get-RequiredDeploymentValue "BOOTSTRAP_ADMIN_PASSWORD"
$BootstrapAdminDisplayName = Get-RequiredDeploymentValue "BOOTSTRAP_ADMIN_DISPLAY_NAME"

& kubectl create namespace $Namespace --dry-run=client -o json | & kubectl apply -f -
Assert-NativeSuccess "无法创建或确认命名空间 $Namespace。"

# StatefulSet PVC 已存在时，MySQL 内部账号仍使用首次初始化的密码；只改 Secret 不会自动改库内密码。
# 升级旧 Chart 时优先读取回滚后仍存在的 opspilot-database，后续 0.3+ 重部署则复用 opspilot-runtime。
$ExistingMysqlPvc = [string](& kubectl -n $Namespace get pvc data-mysql-0 --ignore-not-found -o name)
if (-not [string]::IsNullOrWhiteSpace($ExistingMysqlPvc)) {
    $ExistingDatabaseSecret = [string](& kubectl -n $Namespace get secret opspilot-database --ignore-not-found -o name)
    $ExistingRuntimeSecret = [string](& kubectl -n $Namespace get secret opspilot-runtime --ignore-not-found -o name)
    if (-not [string]::IsNullOrWhiteSpace($ExistingDatabaseSecret)) {
        $CredentialSourceSecret = "opspilot-database"
    }
    elseif (-not [string]::IsNullOrWhiteSpace($ExistingRuntimeSecret)) {
        $CredentialSourceSecret = "opspilot-runtime"
    }
    else {
        throw "检测到既有 MySQL PVC，但没有可匹配的数据库 Secret；为保护数据，脚本拒绝用新密码强行升级。"
    }

    $EncodedDatabasePassword = [string](& kubectl -n $Namespace get secret $CredentialSourceSecret "-o=jsonpath={.data.database-password}")
    $EncodedMysqlRootPassword = [string](& kubectl -n $Namespace get secret $CredentialSourceSecret "-o=jsonpath={.data.mysql-root-password}")
    if ([string]::IsNullOrWhiteSpace($EncodedDatabasePassword) -or [string]::IsNullOrWhiteSpace($EncodedMysqlRootPassword)) {
        throw "既有 Secret $CredentialSourceSecret 缺少 MySQL 凭据键，拒绝覆盖运行 Secret。"
    }
    $DatabasePassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedDatabasePassword))
    $MysqlRootPassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedMysqlRootPassword))
    Write-Host "检测到既有 MySQL PVC；已复用其匹配凭据，不修改数据库数据卷。"
}

# Redis AOF 会把原有数据保留在 PVC 中；直接用 .env 新密码覆盖 Secret 会让
# Redis、API 和 exporter 在滚动期间使用不同凭据。因此既有数据卷必须复用当前 Secret。
$ExistingRedisPvc = [string](& kubectl -n $Namespace get pvc data-opspilot-redis-0 --ignore-not-found -o name)
if (-not [string]::IsNullOrWhiteSpace($ExistingRedisPvc)) {
    $ExistingRedisSecret = [string](& kubectl -n $Namespace get secret opspilot-runtime --ignore-not-found -o name)
    if ([string]::IsNullOrWhiteSpace($ExistingRedisSecret)) {
        throw "检测到既有 Redis PVC，但 opspilot-runtime Secret 已丢失；为避免凭据错位，脚本拒绝覆盖部署。"
    }

    $EncodedRedisPassword = [string](& kubectl -n $Namespace get secret opspilot-runtime "-o=jsonpath={.data.redis-password}")
    if ([string]::IsNullOrWhiteSpace($EncodedRedisPassword)) {
        throw "既有 opspilot-runtime Secret 缺少 redis-password，拒绝覆盖运行 Secret。"
    }
    $RedisPassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($EncodedRedisPassword))
    Write-Host "检测到既有 Redis PVC；已复用当前 Redis 凭据，不进行隐式密码轮换。"
}

# 使用 stdin 传 JSON，避免把密码写进 kubectl 命令行、Shell 历史、Helm values 或仓库文件。
$SecretManifest = [string](@{
    apiVersion = "v1"
    kind = "Secret"
    metadata = @{ name = "opspilot-runtime"; namespace = $Namespace }
    type = "Opaque"
    stringData = @{
        "database-password" = $DatabasePassword
        "mysql-root-password" = $MysqlRootPassword
        "redis-password" = $RedisPassword
        "bootstrap-admin-username" = $BootstrapAdminUsername
        "bootstrap-admin-password" = $BootstrapAdminPassword
        "bootstrap-admin-display-name" = $BootstrapAdminDisplayName
    }
} | ConvertTo-Json -Depth 5 -Compress)
$SecretManifest | & kubectl apply -f -
Assert-NativeSuccess "无法创建 Kubernetes 运行 Secret。"

# 尽早清除脚本变量；Secret 仍由 Kubernetes 保存，脚本后续不再持有明文副本。
$DatabasePassword = $null
$MysqlRootPassword = $null
$RedisPassword = $null
$BootstrapAdminPassword = $null
$SecretManifest = $null
$EncodedDatabasePassword = $null
$EncodedMysqlRootPassword = $null
$EncodedRedisPassword = $null

Write-Host "[4/11] 处理 Ingress 与 Metrics Server 插件"
if ($SkipAddons) {
    Write-Host "已跳过插件启用；HPA 资源仍会创建，但没有 Metrics Server 时 TARGETS 显示 unknown。"
}
else {
    & minikube addons enable ingress
    Assert-NativeSuccess "Ingress 插件启用失败；镜像网络受限时请改用 -SkipAddons -DisableIngress。"
    & minikube addons enable metrics-server
    Assert-NativeSuccess "Metrics Server 启用失败；镜像网络受限时请改用 -SkipAddons。"
}

if (-not $SkipBuild) {
    Write-Host "[5/11] 构建 Java JAR 与 React 生产资源"
    Push-Location $ProjectRoot
    try {
        & mvn -B -ntp -DskipTests package
        Assert-NativeSuccess "Maven 打包失败。"
    }
    finally {
        Pop-Location
    }
    Push-Location (Join-Path $ProjectRoot "frontend")
    try {
        if (-not (Test-Path "node_modules")) {
            & npm ci
            Assert-NativeSuccess "前端依赖还原失败。"
        }
        & npm run build
        Assert-NativeSuccess "React 生产构建失败。"
    }
    finally {
        Pop-Location
    }

    Write-Host "[6/11] 构建版本化运行时镜像"
    & docker build -f (Join-Path $ProjectRoot "Dockerfile.runtime") --build-arg "APP_VERSION=$Version" -t "opspilot-api:$Version" $ProjectRoot
    Assert-NativeSuccess "API 运行时镜像构建失败。"
    & docker build -f (Join-Path $ProjectRoot "frontend\Dockerfile.runtime") -t "opspilot-web:$Version" $ProjectRoot
    Assert-NativeSuccess "Web 运行时镜像构建失败。"

    Write-Host "[7/11] 把 API 与 Web 镜像加载进 Minikube"
    Import-LocalImageToMinikube "opspilot-api:$Version"
    Import-LocalImageToMinikube "opspilot-web:$Version"
}
else {
    Write-Host "[5/11] 已按参数跳过应用构建"
    Write-Host "[6/11] 已按参数跳过运行时镜像构建"
    Write-Host "[7/11] 已按参数跳过 API/Web 镜像加载"
}

Write-Host "[8/11] 把有状态服务、Exporter 与 Release Test 镜像加载进节点"
# 明确加载全部运行依赖，网络受限时不会在 Pod 阶段才暴露 ImagePullBackOff。
@("mysql:8.4", "redis:7.4.10-alpine", "oliver006/redis_exporter:v1.89.0-alpine", "busybox:1.36") |
    ForEach-Object {
        # 全新工作站未必已有依赖镜像；先检查并按固定标签拉取，再通过 archive 导入节点。
        & docker image inspect $_ | Out-Null
        if ($LASTEXITCODE -ne 0) {
            & docker pull $_
            Assert-NativeSuccess "无法拉取运行依赖镜像 $_。"
        }
        Import-LocalImageToMinikube $_
    }

Write-Host "[9/11] 使用 Helm 原子安装或升级 OpsPilot"
# Helm 3 使用 --atomic，Helm 4 将其替换为 --rollback-on-failure。显式检测主版本，
# 避免开发机与 CI 使用不同 Helm 时，部署在参数解析阶段就失败。
$HelmVersion = [string](& helm version --template "{{.Version}}")
Assert-NativeSuccess "无法读取 Helm 版本。"
if ($HelmVersion -notmatch '^v?(?<major>\d+)\.') {
    throw "无法解析 Helm 版本：$HelmVersion"
}
$HelmMajor = [int]$Matches['major']
if ($HelmMajor -eq 3) {
    $HelmFailureFlag = "--atomic"
}
elseif ($HelmMajor -eq 4) {
    $HelmFailureFlag = "--rollback-on-failure"
}
else {
    throw "当前仅支持 Helm 3 或 Helm 4，检测到：$HelmVersion"
}

$HelmArguments = @(
    "upgrade", "--install", "opspilot", $ChartPath,
    "--namespace", $Namespace,
    "--create-namespace",
    "--set-string", "global.version=$Version",
    "--set-string", "api.image.tag=$Version",
    "--set-string", "web.image.tag=$Version",
    # Secret 已由受控 stdin 创建；Helm Release values 不保存明文密钥。
    "--set", "secrets.create=false",
    "--set-string", "secrets.existingSecret=opspilot-runtime",
    $HelmFailureFlag,
    "--wait",
    "--timeout", "8m"
)
if ($DisableIngress) {
    $HelmArguments += @("--set", "ingress.enabled=false")
}
& helm @HelmArguments
Assert-NativeSuccess "Helm 原子发布失败；$HelmFailureFlag 已尝试恢复发布前状态。"

# 本地练习允许在同一个 0.3.0 标签下反复构建。Helm 只比较 Pod 模板，标签未变时不会自动换 Pod；
# 因此仅在本次确实重建并加载了镜像时触发滚动重启，保证运行实例对应刚生成的本地镜像。
if (-not $SkipBuild) {
    & kubectl -n $Namespace rollout restart deployment/opspilot-api deployment/opspilot-web
    Assert-NativeSuccess "无法触发 API/Web 本地镜像滚动更新。"
}

Write-Host "[10/11] 验证 rollout、Release Test 与资源状态"
& kubectl -n $Namespace rollout status deployment/opspilot-api --timeout=180s
Assert-NativeSuccess "API Deployment 未按时就绪。"
& kubectl -n $Namespace rollout status deployment/opspilot-web --timeout=180s
Assert-NativeSuccess "Web Deployment 未按时就绪。"
& helm test opspilot --namespace $Namespace --logs --timeout 2m
Assert-NativeSuccess "Helm Release Test 未通过。"
& kubectl -n $Namespace get pods,svc,ingress,hpa,pdb,pvc,networkpolicy
Assert-NativeSuccess "读取最终 Kubernetes 资源状态失败。"

Write-Host "[11/11] 验证 Redis Service、PVC 与 Exporter"
& kubectl -n $Namespace rollout status statefulset/opspilot-redis --timeout=180s
Assert-NativeSuccess "Redis StatefulSet 未按时就绪。"
& kubectl -n $Namespace rollout status deployment/opspilot-redis-exporter --timeout=180s
Assert-NativeSuccess "Redis Exporter Deployment 未按时就绪。"

$MinikubeIp = & minikube ip
Assert-NativeSuccess "读取 Minikube IP 失败。"
Write-Host ""
Write-Host "部署完成。推荐先运行："
Write-Host "  kubectl -n $Namespace port-forward service/opspilot-web 18000:8080"
Write-Host "  然后访问 http://localhost:18000"
if (-not $DisableIngress) {
    Write-Host "Ingress 演示地址：在 hosts 中加入 '$MinikubeIp opspilot.local' 后访问 http://opspilot.local"
}
