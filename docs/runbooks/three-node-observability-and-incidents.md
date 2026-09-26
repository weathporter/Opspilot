# NorthLedger Kubernetes 监控与故障记录（两节点主线，三节点可复用）

> 本文是操作流程，不是已经完成的故障案例。命令在 `nl-cp1` 上执行，除非特别标出工作节点。先用[两节点发布手册](two-node-microservices-release.md)让业务和 PVC 正常，再装监控。三节点扩容后步骤相同，但告警阈值与容量要重新测量。

## 1. 五层观察对象，分别回答什么问题

| 层 | 工具和证据 | 回答的问题 | 不能证明什么 |
| --- | --- | --- | --- |
| 节点 | `kubectl get/describe node`、node-exporter | 主机 Ready、CPU/内存压力 | 交易接口一定正常 |
| Kubernetes 对象 | Deployment、Pod、PVC、Events、kube-state-metrics | 期望副本、就绪副本、调度/挂载错误 | 用户请求一定成功 |
| 应用 | `/actuator/prometheus`、应用日志 | 请求数、5xx、JVM 与代码异常 | 下游数据一定一致 |
| 监控系统 | Prometheus Targets/Rules、Alertmanager | 指标能否被抓取、规则是否触发 | 告警已通知到某个人 |
| 用户路径 | 浏览器及冒烟脚本 | 登录、账户、交易/对账流程 | 所有异常场景都覆盖 |

因此故障调查不能只贴一张 Grafana 图。`Target UP` 只说明抓取成功；用户路径要单独验证。Prometheus 负责拉取指标和计算规则，Alertmanager 负责接收、分组、静默与路由告警。本实验档位**只提供 Alertmanager UI，未配置邮件/IM 接收人**，不要写“已实现自动通知”。

## 2. 安装集群内监控

配置源在 `deploy/k8s/monitoring/`；Prometheus Chart 固定 `29.33.1`，Grafana Chart 固定 `13.2.5`。这套组件装在 `monitoring` 命名空间，不使用 Compose 的 `localhost:19090` 容器。首次安装需能拉取 Helm Chart 和容器镜像；若镜像网络失败，先记录 `kubectl describe pod` 中的 Events，不要把 `ImagePullBackOff` 写成代码故障。

```bash
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update
helm template northledger-metrics prometheus-community/prometheus --version 29.33.1 -n monitoring -f deploy/k8s/monitoring/prometheus-values.yaml >/dev/null
helm template northledger-grafana grafana-community/grafana --version 13.2.5 -n monitoring -f deploy/k8s/monitoring/grafana-values.yaml --set-file dashboards.default.northledger.json=deploy/k8s/monitoring/northledger-dashboard.json >/dev/null
```

Grafana 管理员密码由操作者现场输入，不提交到 Git。以下在 Bash 的交互终端执行，输入不会显示；不要开启 `set -x`，也不要截屏输入过程。`/dev/stdin` 让密码不出现在命令行参数中。

```bash
set -o pipefail
read -r -s -p 'Grafana admin password: ' GRAFANA_PASSWORD
printf '\n'
printf '%s' "$GRAFANA_PASSWORD" | kubectl -n monitoring create secret generic northledger-grafana-admin --from-literal=admin-user=admin --from-file=admin-password=/dev/stdin --dry-run=client -o yaml | kubectl apply -f -
unset GRAFANA_PASSWORD
```

然后安装。固定版本不是自动升级承诺；升级图表需重新做渲染与目标检查。

```bash
helm upgrade --install northledger-metrics prometheus-community/prometheus --version 29.33.1 -n monitoring --atomic --wait --timeout 10m -f deploy/k8s/monitoring/prometheus-values.yaml
helm upgrade --install northledger-grafana grafana-community/grafana --version 13.2.5 -n monitoring --atomic --wait --timeout 10m -f deploy/k8s/monitoring/grafana-values.yaml --set-file dashboards.default.northledger.json=deploy/k8s/monitoring/northledger-dashboard.json
kubectl -n monitoring get pods,svc -o wide
```

若资源不足，先看 `kubectl describe pod` 的 `Insufficient memory`、`kubectl describe node` 的 Allocatable/Allocated 与宿主机内存，不要盲目提高 requests 或解除控制平面污点。这个 profile 将 Prometheus、Alertmanager、Grafana 数据放在临时卷：Pod 替换后历史指标、静默状态与 Grafana UI 中未导出的面板可能丢失；这是轻量实验档位，不能声称监控高可用或持久化。MySQL/Redis 的 Local PV 与此不同，**不要为腾空间删除业务 PVC/PV**。

## 3. 从 Windows 打开 Web UI

先查实际服务名与端口，再在 `nl-cp1` 分别开三个终端做转发：

```bash
kubectl -n monitoring get svc
kubectl -n monitoring port-forward svc/northledger-prometheus 19090:80
kubectl -n monitoring port-forward svc/northledger-grafana 13000:80
# Alertmanager 的 Service 名以 get svc 输出为准，示例：
kubectl -n monitoring port-forward svc/northledger-metrics-alertmanager 19093:9093
```

VM 上的 `127.0.0.1` **不是** Windows 的 `127.0.0.1`。在 FinalShell 为三个端口配置 SSH 本地转发（本地 19090/13000/19093 → 远端 127.0.0.1 同端口），然后在 Windows 浏览器分别访问 `http://127.0.0.1:19090/targets`、`http://127.0.0.1:13000/`、`http://127.0.0.1:19093/#/alerts`。不要将无认证的 Prometheus/Alertmanager 直接暴露到校园网或公网。

## 4. 验收指标链路

1. `kubectl -n monitoring get pods -o wide`：Prometheus、Alertmanager、Grafana、kube-state-metrics 就绪；node-exporter DaemonSet 对每个节点有 1 个 Pod。控制平面只为 node-exporter 配了定向污点容忍，业务 Pod 不会因此调度到控制平面。
2. Prometheus `/targets`：Access、Ledger、Operations **三个 Java Pod** 的 `/actuator/prometheus`、两个 node-exporter、kube-state-metrics 均为 `UP`。Web 是 Nginx，没有 `/actuator/prometheus`；它的可用性看 `kube_deployment_status_replicas_available{deployment="northledger-web"}`、readiness 与用户入口冒烟。若目标缺失，先核对 Pod 注解与发现规则；若 `DOWN`，点开 Last Error，再看 NetworkPolicy/端口/接口响应。
3. Prometheus 查询 `up{namespace="northledger"}`、`kube_deployment_status_replicas_available{namespace="northledger"}`、`node_memory_MemAvailable_bytes`；无序列时先检查采集组件，不要把“0”与“没数据”混为一谈。
4. Grafana 打开随仓库发布的 `NorthLedger · Kubernetes operations` 看板，核对期望/可用副本、Target、节点内存与 HTTP 5xx；Explore 选择预置 Prometheus 数据源执行相同查询。看板的 `No data` 不等于数值 0；若 Prometheus 有数而 Grafana 无数，检查数据源 URL、Grafana Pod DNS 和 Service。
5. 在 Prometheus `/rules` 看规则为 `inactive`/`pending`/`firing`，在 Alertmanager 确认收到告警。没有故障时 `inactive` 是正常的，不代表规则没加载。

规则阈值是**实验室起点**：Deployment 3 分钟不齐、Target 3 分钟 DOWN、10 分钟内重启超过 2 次、节点可用内存低于 10% 5 分钟。它们不是生产 SLO；正式环境需基于业务流量与误报数据调整。

## 5. 固定排查顺序：现象 → 证据 → 假设 → 恢复

下面是“Access 页面 503”的示例调查顺序；不要跳到最后一句断定 MySQL 坏了。

```bash
date -Is
kubectl -n northledger get deploy,sts,pod,svc,pvc -o wide
kubectl -n northledger get events --sort-by=.lastTimestamp
kubectl -n northledger describe deployment northledger-access
kubectl -n northledger get endpointslice -l kubernetes.io/service-name=northledger-access
kubectl -n northledger logs deploy/northledger-access --tail=100 --timestamps
kubectl -n northledger get pod -l app.kubernetes.io/component=access -o wide
```

为什么这样排：`get` 定影响面；Events/`describe` 看调度、镜像、探针、挂载；EndpointSlice 判断 Service 有没有就绪后端；日志看应用报错。如果容器曾重启，针对实际 Pod 名运行 `kubectl logs POD_NAME --previous --timestamps`，当前日志可能已经丢掉异常前的最后几行。若 Pod `Running` 但 readiness 失败，应看探针返回码和下游依赖；`Running` 不等于 `Ready`。

常见分支：

| 现象 | 先查 | 可能原因 | 最小恢复动作（确认后） |
| --- | --- | --- | --- |
| `Pending` | Pod Events、PVC、nodeAffinity、Allocated resources | worker 资源不足或 Local PV 在另一节点 | 修正容量/节点与存储规划；已有数据先备份，不迁移 PV 标签冒充搬迁 |
| `ImagePullBackOff` | Pod Events 的仓库错误 | 标签不存在、凭据失效、网络受阻 | 核对同一 SHA 镜像及 pull secret；修复网络/凭据后观察自动重试 |
| `CrashLoopBackOff` | `logs --previous`、退出码、Secret 引用 | 应用启动失败、Flyway 或配置错误 | 修配置或回滚确切 Helm revision；不删 MySQL PVC |
| Target `DOWN` | Prometheus Last Error、Pod 注解、NetworkPolicy | 指标路径、端口或抓取网络不通 | 只修改对应配置并复测 Target；同时做用户侧冒烟 |
| 503 但 Target `UP` | Ingress、Service、EndpointSlice、浏览器网络路径 | 入口或路由问题 | 修入口/后端选择器；`UP` 不能替代登录复测 |
| 节点 `NotReady` | `describe node`、`journalctl -u kubelet`、磁盘/内存 | kubelet、CNI、磁盘或内存压力 | 先恢复节点健康；不要靠删除 StatefulSet 数据掩盖问题 |

执行任何会改变流量或数据的动作前，先记下 Helm revision、当前就绪副本、MySQL/Redis PVC 状态和是否存在写入。恢复后至少做三类检查：Kubernetes 对象恢复就绪、监控告警清除、用户路径冒烟成功。若只是 Pod 重启后 1 分钟绿了，写“短时恢复，持续观察中”，不要写“根治”。

## 6. 一份真实、可追问的记录长什么样

复制[事件模板](../incidents/template.md)填写。建议每条结论都带“反证”：例如认定是镜像不存在，要有 Events 中的 `manifest unknown`，而不是因为 Pod 处于 `ImagePullBackOff` 就猜网络；认定是网络策略阻断，要有同一目的地址/端口的连通性对照和策略修改后的复测。每条恢复结论至少链接一条用户路径结果，而不只是一条 `kubectl get pods`。

你先前 `kubeadm init` 截图中可确认的只是**当时**控制平面内存 1674 MiB 低于 1700 MiB、`net.ipv4.ip_forward=0`、kubelet 未启用警告和内核版本警告；截图不能证明后来是否修复，也不能证明集群已 Ready。这个例子正适合练习区分“原始现象”与“最终结论”。
