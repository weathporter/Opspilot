#!/usr/bin/env bash
# OpsPilot 三节点 Kubernetes 学习环境只读体检。
# 本脚本只执行 get、version、cluster-info、api-resources、auth can-i 等查询，
# 不会创建、修改或删除任何集群资源。

set -u

section() {
  printf '\n========== %s ==========\n' "$1"
}

run() {
  printf '\n$ %s\n' "$*"
  "$@" 2>&1 || printf '[WARN] 命令未成功，保留输出供后续分析。\n'
}

if ! command -v kubectl >/dev/null 2>&1; then
  printf 'ERROR: 当前终端找不到 kubectl，请在能够管理集群的 master 终端执行。\n' >&2
  exit 1
fi

section "1. Kubernetes 客户端与服务端版本"
run kubectl version

section "2. 集群入口与节点"
run kubectl cluster-info
run kubectl get nodes -o wide
run kubectl get nodes -o 'custom-columns=NAME:.metadata.name,KUBELET:.status.nodeInfo.kubeletVersion,RUNTIME:.status.nodeInfo.containerRuntimeVersion,OS:.status.nodeInfo.osImage'

section "3. 节点标签与污点"
run kubectl get nodes --show-labels
run kubectl get nodes -o 'custom-columns=NAME:.metadata.name,TAINTS:.spec.taints'

section "4. 系统组件与跨节点 Pod 分布"
run kubectl get pods -A -o wide
run kubectl get daemonset -n kube-system -o wide
run kubectl get deployment -n kube-system -o wide

section "5. Service、EndpointSlice 与 Ingress"
run kubectl get service -A
run kubectl get endpointslice -A
run kubectl get ingressclass
run kubectl get ingress -A

section "6. 存储能力"
run kubectl get storageclass
run kubectl get persistentvolume
run kubectl get persistentvolumeclaim -A

section "7. Metrics Server 与 HPA API"
run kubectl get apiservice v1beta1.metrics.k8s.io
run kubectl top nodes
run kubectl api-resources --api-group=autoscaling

section "8. 网络策略与常见 CNI 线索"
run kubectl api-resources --api-group=networking.k8s.io
run kubectl get customresourcedefinition

section "9. 当前账号的学习操作权限"
run kubectl auth can-i create namespace
run kubectl auth can-i create deployment --namespace default
run kubectl auth can-i create statefulset --namespace default
run kubectl auth can-i create networkpolicy --namespace default

section "10. Helm（如果已经安装）"
if command -v helm >/dev/null 2>&1; then
  run helm version
else
  printf '\n[INFO] 当前终端没有 Helm；这不是集群故障，后续发布阶段再安装。\n'
fi

printf '\n========== 体检结束 ==========\n'
printf '请把从“1. Kubernetes 客户端与服务端版本”到这里的完整输出发给老师。\n'
