[CmdletBinding()]
param(
    [string]$Version = "0.1.1",
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

@("docker", "minikube", "kubectl", "helm", "mvn", "npm") |
    ForEach-Object { Assert-Command $_ }

Write-Host "[1/9] 检查 Docker Desktop 引擎"
& docker info | Out-Null
Assert-NativeSuccess "Docker Desktop 不可用，请先启动引擎。"

Write-Host "[2/9] 确保 Minikube 集群运行"
$MinikubeState = & minikube status --format "{{.Host}}" 2>$null
if ($LASTEXITCODE -ne 0 -or $MinikubeState -ne "Running") {
    # 已存在的 profile 会保留原资源配置；脚本不会擅自删除用户集群。
    & minikube start --driver=docker --cpus=4 --memory=6144
    Assert-NativeSuccess "Minikube 启动失败。"
}

Write-Host "[3/9] 处理 Ingress 与 Metrics Server 插件"
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
    Write-Host "[4/9] 构建 Java JAR 与 React 生产资源"
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

    Write-Host "[5/9] 构建版本化运行时镜像"
    & docker build -f (Join-Path $ProjectRoot "Dockerfile.runtime") --build-arg "APP_VERSION=$Version" -t "opspilot-api:$Version" $ProjectRoot
    Assert-NativeSuccess "API 运行时镜像构建失败。"
    & docker build -f (Join-Path $ProjectRoot "frontend\Dockerfile.runtime") -t "opspilot-web:$Version" $ProjectRoot
    Assert-NativeSuccess "Web 运行时镜像构建失败。"

    Write-Host "[6/9] 把 API 与 Web 镜像加载进 Minikube"
    & minikube image load "opspilot-api:$Version"
    Assert-NativeSuccess "API 镜像加载失败。"
    & minikube image load "opspilot-web:$Version"
    Assert-NativeSuccess "Web 镜像加载失败。"
}
else {
    Write-Host "[4/9] 已按参数跳过应用构建"
    Write-Host "[5/9] 已按参数跳过运行时镜像构建"
    Write-Host "[6/9] 已按参数跳过 API/Web 镜像加载"
}

Write-Host "[7/9] 确保学习用 MySQL 镜像已进入节点"
# 只加载自研镜像仍可能因外部镜像站故障导致 MySQL ImagePullBackOff，因此显式加载第三个运行依赖。
& minikube image load "mysql:8.4"
Assert-NativeSuccess "MySQL 8.4 镜像加载失败；请先在 Docker Desktop 中准备 mysql:8.4。"

Write-Host "[8/9] 使用 Helm 原子安装或升级 OpsPilot"
$HelmArguments = @(
    "upgrade", "--install", "opspilot", $ChartPath,
    "--namespace", $Namespace,
    "--create-namespace",
    "--set-string", "global.version=$Version",
    "--set-string", "api.image.tag=$Version",
    "--set-string", "web.image.tag=$Version",
    "--rollback-on-failure",
    "--wait",
    "--timeout", "8m"
)
if ($DisableIngress) {
    $HelmArguments += @("--set", "ingress.enabled=false")
}
& helm @HelmArguments
Assert-NativeSuccess "Helm 发布失败；--rollback-on-failure 已尝试恢复发布前状态。"

Write-Host "[9/9] 验证 rollout、Release Test 与资源状态"
& kubectl -n $Namespace rollout status deployment/opspilot-api --timeout=180s
Assert-NativeSuccess "API Deployment 未按时就绪。"
& kubectl -n $Namespace rollout status deployment/opspilot-web --timeout=180s
Assert-NativeSuccess "Web Deployment 未按时就绪。"
& helm test opspilot --namespace $Namespace --logs --timeout 2m
Assert-NativeSuccess "Helm Release Test 未通过。"
& kubectl -n $Namespace get pods,svc,ingress,hpa,pdb,pvc,networkpolicy
Assert-NativeSuccess "读取最终 Kubernetes 资源状态失败。"

$MinikubeIp = & minikube ip
Assert-NativeSuccess "读取 Minikube IP 失败。"
Write-Host ""
Write-Host "部署完成。推荐先运行："
Write-Host "  kubectl -n $Namespace port-forward service/opspilot-web 18000:8080"
Write-Host "  然后访问 http://localhost:18000"
if (-not $DisableIngress) {
    Write-Host "Ingress 演示地址：在 hosts 中加入 '$MinikubeIp opspilot.local' 后访问 http://opspilot.local"
}
