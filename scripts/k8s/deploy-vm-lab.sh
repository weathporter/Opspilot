#!/usr/bin/env bash
# 在预检、镜像导入、Secret 和本地存储准备完成后安装 OpsPilot。
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LAB_ROOT="${1:-${SCRIPT_DIR}/../../deploy/k8s/vm-lab}"
NAMESPACE="opspilot"

[[ -f "${LAB_ROOT}/kustomization.yaml" ]] || {
  printf 'ERROR: 找不到 %s/kustomization.yaml\n' "${LAB_ROOT}" >&2
  exit 1
}

kubectl get secret opspilot-database -n "${NAMESPACE}" >/dev/null 2>&1 || {
  printf 'ERROR: 缺少 Secret opspilot/%s，请先按 README 使用交互式变量创建。\n' "opspilot-database" >&2
  exit 1
}

for worker_name in k8s-node1 k8s-node2; do
  worker_label="$(kubectl get node "${worker_name}" -o jsonpath='{.metadata.labels.opspilot\.io/workload}')"
  [[ "${worker_label}" == "true" ]] || {
    printf 'ERROR: 节点 %s 缺少 opspilot.io/workload=true 标签。\n' "${worker_name}" >&2
    exit 1
  }
done

kubectl apply -k "${LAB_ROOT}"

kubectl wait --for=condition=Ready pod/mysql-0 -n "${NAMESPACE}" --timeout=300s
kubectl rollout status deployment/opspilot-api -n "${NAMESPACE}" --timeout=300s
kubectl rollout status deployment/opspilot-web -n "${NAMESPACE}" --timeout=300s

printf '\nOpsPilot 工作负载：\n'
kubectl get pod,service,persistentvolumeclaim -n "${NAMESPACE}" -o wide
printf '\n访问地址：http://192.168.99.101:30080 或 http://192.168.99.102:30080\n'
