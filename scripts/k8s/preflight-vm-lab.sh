#!/usr/bin/env bash
# 只读预检：确认该部署包运行在教程对应的 K8s 1.21 / Docker / Calico 三节点实验集群。
set -u

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

command -v kubectl >/dev/null 2>&1 || fail "找不到 kubectl，请在 k8s-master 上执行。"
kubectl cluster-info >/dev/null 2>&1 || fail "kubectl 无法连接 API Server。"

server_version="$(kubectl version --short 2>/dev/null | awk '/Server Version/{print $3}')"
[[ "${server_version}" == v1.21.* ]] || fail "当前服务端是 ${server_version:-unknown}，本目录只用于 Kubernetes v1.21 实验集群。其他版本请使用 deploy/k8s/helm/opspilot。"

for node_name in k8s-master k8s-node1 k8s-node2; do
  kubectl get node "${node_name}" >/dev/null 2>&1 || fail "缺少节点 ${node_name}。"
  ready="$(kubectl get node "${node_name}" -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}')"
  [[ "${ready}" == "True" ]] || fail "节点 ${node_name} 不是 Ready。"
done

for worker_name in k8s-node1 k8s-node2; do
  runtime="$(kubectl get node "${worker_name}" -o jsonpath='{.status.nodeInfo.containerRuntimeVersion}')"
  [[ "${runtime}" == docker://* ]] || fail "${worker_name} 的运行时是 ${runtime}，不能使用本教程的 docker load 镜像分发步骤。"
done

kubectl -n kube-system get daemonset calico-node >/dev/null 2>&1 || fail "没有发现 calico-node DaemonSet，暂不应应用本部署包的 NetworkPolicy。"
pdb_discovery="$(kubectl get --raw /apis/policy/v1 2>/dev/null || true)"
grep -q '"name":"poddisruptionbudgets"' <<<"${pdb_discovery}" || fail "API Server 的 policy/v1 发现接口中没有 poddisruptionbudgets。"

printf 'PASS: Kubernetes=%s，三节点 Ready，worker 使用 Docker，Calico 与 policy/v1 PDB 可用。\n' "${server_version}"
kubectl get nodes -o wide
