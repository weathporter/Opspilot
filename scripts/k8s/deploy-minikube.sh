#!/usr/bin/env bash
# Linux/macOS 版一键部署脚本；与 PowerShell 版执行相同的安全注入、构建、发布、回滚和验收流程。
set -Eeuo pipefail

VERSION="${1:-0.3.0}"
NAMESPACE="${NAMESPACE:-opspilot}"
SKIP_BUILD="${SKIP_BUILD:-false}"
SKIP_ADDONS="${SKIP_ADDONS:-false}"
DISABLE_INGRESS="${DISABLE_INGRESS:-false}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
CHART_PATH="${PROJECT_ROOT}/deploy/k8s/helm/opspilot"
LOCAL_ENV_PATH="${PROJECT_ROOT}/.env"

for command_name in docker minikube kubectl helm mvn npm base64 mktemp; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "缺少命令: ${command_name}" >&2
    exit 1
  }
done

# 先读进程环境；本机学习环境才回退到被 .gitignore 排除的 .env，且不执行其中的任意内容。
read_required_value() {
  local name="$1"
  local value="${!name:-}"
  if [[ -z "${value}" && -f "${LOCAL_ENV_PATH}" ]]; then
    value="$(awk -F= -v key="${name}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${LOCAL_ENV_PATH}")"
  fi
  if [[ -z "${value}" ]]; then
    echo "缺少 ${name}。请在当前进程环境或已忽略的 .env 中设置后再部署。" >&2
    exit 1
  fi
  printf '%s' "${value}"
}

# GNU coreutils 使用 --decode，macOS 使用 -D；函数只把结果保存在调用方变量中，不打印日志。
decode_base64() {
  local encoded="$1"
  if printf '%s' "${encoded}" | base64 --decode 2>/dev/null; then
    return 0
  fi
  printf '%s' "${encoded}" | base64 -D
}

# 通过标准 Docker archive 传输，避免 BuildKit manifest list 与节点同标签缓存导致旧镜像继续运行。
import_local_image_to_minikube() {
  local image_name="$1"
  local transfer_dir archive transfer_status
  transfer_dir="$(mktemp -d "${TMPDIR:-/tmp}/northledger-image-transfer.XXXXXX")"
  archive="${transfer_dir}/image.tar"
  transfer_status=0
  docker image save --output "${archive}" "${image_name}" || transfer_status=$?
  if [[ ${transfer_status} -eq 0 ]]; then
    minikube image load --overwrite=true "${archive}" || transfer_status=$?
  fi
  rm -f -- "${archive}"
  rmdir -- "${transfer_dir}" 2>/dev/null || true
  if [[ ${transfer_status} -ne 0 ]]; then
    echo "无法把 Docker 镜像 ${image_name} 加载到 Minikube。" >&2
    return "${transfer_status}"
  fi
}

echo "[1/11] 检查 Docker Desktop 引擎"
docker info >/dev/null

echo "[2/11] 确保 Minikube 集群运行"
if [[ "$(minikube status --format='{{.Host}}' 2>/dev/null || true)" != "Running" ]]; then
  minikube start --driver=docker --cpus=4 --memory=6144
fi

echo "[3/11] 创建命名空间并安全注入运行密钥"
database_password="$(read_required_value DB_PASSWORD)"
mysql_root_password="$(read_required_value MYSQL_ROOT_PASSWORD)"
redis_password="$(read_required_value REDIS_PASSWORD)"
bootstrap_admin_username="$(read_required_value BOOTSTRAP_ADMIN_USERNAME)"
bootstrap_admin_password="$(read_required_value BOOTSTRAP_ADMIN_PASSWORD)"
bootstrap_admin_display_name="$(read_required_value BOOTSTRAP_ADMIN_DISPLAY_NAME)"

kubectl create namespace "${NAMESPACE}" --dry-run=client -o json | kubectl apply -f -

# StatefulSet PVC 保留首次初始化的 MySQL 密码；升级时必须复用匹配 Secret，不能只换配置值。
if [[ -n "$(kubectl -n "${NAMESPACE}" get pvc data-mysql-0 --ignore-not-found -o name)" ]]; then
  if [[ -n "$(kubectl -n "${NAMESPACE}" get secret opspilot-database --ignore-not-found -o name)" ]]; then
    credential_source_secret="opspilot-database"
  elif [[ -n "$(kubectl -n "${NAMESPACE}" get secret opspilot-runtime --ignore-not-found -o name)" ]]; then
    credential_source_secret="opspilot-runtime"
  else
    echo "检测到既有 MySQL PVC，但没有匹配的数据库 Secret；为保护数据，拒绝用新密码强行升级。" >&2
    exit 1
  fi

  encoded_database_password="$(kubectl -n "${NAMESPACE}" get secret "${credential_source_secret}" -o 'jsonpath={.data.database-password}')"
  encoded_mysql_root_password="$(kubectl -n "${NAMESPACE}" get secret "${credential_source_secret}" -o 'jsonpath={.data.mysql-root-password}')"
  if [[ -z "${encoded_database_password}" || -z "${encoded_mysql_root_password}" ]]; then
    echo "既有 Secret ${credential_source_secret} 缺少 MySQL 凭据键，拒绝覆盖运行 Secret。" >&2
    exit 1
  fi
  database_password="$(decode_base64 "${encoded_database_password}")"
  mysql_root_password="$(decode_base64 "${encoded_mysql_root_password}")"
  echo "检测到既有 MySQL PVC；已复用其匹配凭据，不修改数据库数据卷。"
fi

# Redis AOF 与 PVC 保留了当前认证状态。数据卷存在时不接受 .env 中的新密码，
# 避免 Redis、API 与 exporter 在滚动发布中使用不同凭据。
if [[ -n "$(kubectl -n "${NAMESPACE}" get pvc data-opspilot-redis-0 --ignore-not-found -o name)" ]]; then
  if [[ -z "$(kubectl -n "${NAMESPACE}" get secret opspilot-runtime --ignore-not-found -o name)" ]]; then
    echo "检测到既有 Redis PVC，但 opspilot-runtime Secret 已丢失；为避免凭据错位，拒绝覆盖部署。" >&2
    exit 1
  fi
  encoded_redis_password="$(kubectl -n "${NAMESPACE}" get secret opspilot-runtime -o 'jsonpath={.data.redis-password}')"
  if [[ -z "${encoded_redis_password}" ]]; then
    echo "既有 opspilot-runtime Secret 缺少 redis-password，拒绝覆盖运行 Secret。" >&2
    exit 1
  fi
  redis_password="$(decode_base64 "${encoded_redis_password}")"
  echo "检测到既有 Redis PVC；已复用当前 Redis 凭据，不进行隐式密码轮换。"
fi

# 密钥写入权限为 0700 的临时目录，通过 --from-file 和 stdin 创建 Secret；进程参数与 Helm values 不含明文。
secret_temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/northledger-k8s.XXXXXX")"
cleanup_secret_files() {
  rm -f -- "${secret_temp_dir}/database-password" \
    "${secret_temp_dir}/mysql-root-password" \
    "${secret_temp_dir}/redis-password" \
    "${secret_temp_dir}/bootstrap-admin-username" \
    "${secret_temp_dir}/bootstrap-admin-password" \
    "${secret_temp_dir}/bootstrap-admin-display-name"
  rmdir -- "${secret_temp_dir}" 2>/dev/null || true
}
trap cleanup_secret_files EXIT
chmod 700 "${secret_temp_dir}"
printf '%s' "${database_password}" > "${secret_temp_dir}/database-password"
printf '%s' "${mysql_root_password}" > "${secret_temp_dir}/mysql-root-password"
printf '%s' "${redis_password}" > "${secret_temp_dir}/redis-password"
printf '%s' "${bootstrap_admin_username}" > "${secret_temp_dir}/bootstrap-admin-username"
printf '%s' "${bootstrap_admin_password}" > "${secret_temp_dir}/bootstrap-admin-password"
printf '%s' "${bootstrap_admin_display_name}" > "${secret_temp_dir}/bootstrap-admin-display-name"
kubectl -n "${NAMESPACE}" create secret generic opspilot-runtime \
  --from-file="database-password=${secret_temp_dir}/database-password" \
  --from-file="mysql-root-password=${secret_temp_dir}/mysql-root-password" \
  --from-file="redis-password=${secret_temp_dir}/redis-password" \
  --from-file="bootstrap-admin-username=${secret_temp_dir}/bootstrap-admin-username" \
  --from-file="bootstrap-admin-password=${secret_temp_dir}/bootstrap-admin-password" \
  --from-file="bootstrap-admin-display-name=${secret_temp_dir}/bootstrap-admin-display-name" \
  --dry-run=client -o json | kubectl apply -f -
cleanup_secret_files
trap - EXIT
unset database_password mysql_root_password redis_password bootstrap_admin_password
unset encoded_database_password encoded_mysql_root_password encoded_redis_password

echo "[4/11] 处理 Ingress 与 Metrics Server 插件"
if [[ "${SKIP_ADDONS}" == "true" ]]; then
  echo "已跳过插件启用；没有 Metrics Server 时 HPA TARGETS 会显示 unknown。"
else
  minikube addons enable ingress
  minikube addons enable metrics-server
fi

if [[ "${SKIP_BUILD}" == "true" ]]; then
  echo "[5/11] 已按环境变量跳过应用构建"
  echo "[6/11] 已按环境变量跳过运行时镜像构建"
  echo "[7/11] 已按环境变量跳过 API/Web 镜像加载"
else
  echo "[5/11] 构建 Java JAR 与 React 生产资源"
  (cd "${PROJECT_ROOT}" && mvn -B -ntp -DskipTests package)
  (cd "${PROJECT_ROOT}/frontend" && { [[ -d node_modules ]] || npm ci; } && npm run build)

  echo "[6/11] 构建版本化运行时镜像"
  docker build -f "${PROJECT_ROOT}/Dockerfile.runtime" --build-arg "APP_VERSION=${VERSION}" -t "opspilot-api:${VERSION}" "${PROJECT_ROOT}"
  docker build -f "${PROJECT_ROOT}/frontend/Dockerfile.runtime" -t "opspilot-web:${VERSION}" "${PROJECT_ROOT}"

  echo "[7/11] 把 API 与 Web 镜像加载进 Minikube"
  import_local_image_to_minikube "opspilot-api:${VERSION}"
  import_local_image_to_minikube "opspilot-web:${VERSION}"
fi

echo "[8/11] 把有状态服务、Exporter 与 Release Test 镜像加载进节点"
for image_name in mysql:8.4 redis:7.4.10-alpine oliver006/redis_exporter:v1.89.0-alpine busybox:1.36; do
  # 新工作站缺少依赖镜像时先拉取固定标签，再以可验证 archive 方式载入节点。
  docker image inspect "${image_name}" >/dev/null 2>&1 || docker pull "${image_name}"
  import_local_image_to_minikube "${image_name}"
done

echo "[9/11] 使用 Helm 原子安装或升级 NorthLedger"
# Helm 3 使用 --atomic，Helm 4 使用改名后的 --rollback-on-failure；其他主版本先停止而不是猜测。
helm_version="$(helm version --template '{{.Version}}')"
case "${helm_version#v}" in
  3.*) helm_failure_flag="--atomic" ;;
  4.*) helm_failure_flag="--rollback-on-failure" ;;
  *)
    echo "当前仅支持 Helm 3 或 Helm 4，检测到：${helm_version}" >&2
    exit 1
    ;;
esac
helm_arguments=(
  upgrade --install opspilot "${CHART_PATH}"
  --namespace "${NAMESPACE}"
  --create-namespace
  --set-string "global.version=${VERSION}"
  --set-string "api.image.tag=${VERSION}"
  --set-string "web.image.tag=${VERSION}"
  --set secrets.create=false
  --set-string secrets.existingSecret=opspilot-runtime
  "${helm_failure_flag}"
  --wait
  --timeout 8m
)
if [[ "${DISABLE_INGRESS}" == "true" ]]; then
  helm_arguments+=(--set ingress.enabled=false)
fi
helm "${helm_arguments[@]}"

# 同一开发版本标签被重新构建时，Helm 看不到 Pod 模板变化；主动滚动才能确保新 Pod 使用刚加载的镜像。
if [[ "${SKIP_BUILD}" != "true" ]]; then
  kubectl -n "${NAMESPACE}" rollout restart deployment/opspilot-api deployment/opspilot-web
fi

echo "[10/11] 验证 rollout、Release Test 与资源状态"
kubectl -n "${NAMESPACE}" rollout status deployment/opspilot-api --timeout=180s
kubectl -n "${NAMESPACE}" rollout status deployment/opspilot-web --timeout=180s
helm test opspilot --namespace "${NAMESPACE}" --logs --timeout 2m
kubectl -n "${NAMESPACE}" get pods,svc,ingress,hpa,pdb,pvc,networkpolicy

echo "[11/11] 验证 Redis Service、PVC 与 Exporter"
kubectl -n "${NAMESPACE}" rollout status statefulset/opspilot-redis --timeout=180s
kubectl -n "${NAMESPACE}" rollout status deployment/opspilot-redis-exporter --timeout=180s

minikube_ip="$(minikube ip)"
echo
echo "部署完成。推荐先运行："
echo "  kubectl -n ${NAMESPACE} port-forward service/opspilot-web 18000:8080"
echo "  然后访问 http://localhost:18000"
if [[ "${DISABLE_INGRESS}" != "true" ]]; then
  echo "Ingress 演示地址：在 hosts 中加入 '${minikube_ip} opspilot.local' 后访问 http://opspilot.local"
fi
