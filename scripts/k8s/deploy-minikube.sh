#!/usr/bin/env bash
# Linux/macOS 版一键部署脚本；与 PowerShell 版执行相同的构建、加载、Helm 升级和验收步骤。
set -Eeuo pipefail

VERSION="${1:-0.2.0}"
NAMESPACE="${NAMESPACE:-opspilot}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
CHART_PATH="${PROJECT_ROOT}/deploy/k8s/helm/opspilot"

for command_name in docker minikube kubectl helm; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "缺少命令: ${command_name}" >&2
    exit 1
  }
done

docker info >/dev/null
if [[ "$(minikube status --format='{{.Host}}' 2>/dev/null || true)" != "Running" ]]; then
  minikube start --driver=docker --cpus=4 --memory=6144
fi

minikube addons enable ingress
minikube addons enable metrics-server

docker build --build-arg "APP_VERSION=${VERSION}" -t "opspilot-api:${VERSION}" "${PROJECT_ROOT}"
(
  cd "${PROJECT_ROOT}/frontend"
  [[ -d node_modules ]] || npm ci
  npm run build
)
docker build -f "${PROJECT_ROOT}/frontend/Dockerfile.runtime" -t "opspilot-web:${VERSION}" "${PROJECT_ROOT}"
minikube image load "opspilot-api:${VERSION}"
minikube image load "opspilot-web:${VERSION}"

helm upgrade --install opspilot "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  --set-string "global.version=${VERSION}" \
  --set-string "api.image.tag=${VERSION}" \
  --set-string "web.image.tag=${VERSION}" \
  --wait \
  --timeout 8m

kubectl -n "${NAMESPACE}" rollout status deployment/opspilot-api --timeout=180s
kubectl -n "${NAMESPACE}" rollout status deployment/opspilot-web --timeout=180s
kubectl -n "${NAMESPACE}" get pods,svc,ingress,hpa,pdb
