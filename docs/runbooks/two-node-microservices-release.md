# NorthLedger 两节点 Kubernetes 实验部署

> 状态：可执行的操作手册，不代表已在你的虚拟机上部署。所有 `kubectl`/`helm` 集群命令由你在 `nl-cp1` 或经授权的发布机执行。原三节点配置仍保留为扩展方案；**两份 Local PV 清单只能选择一份，不能对已有 PV 直接改节点亲和性。**

## 为什么先做两节点

本机读到的物理内存约 15.2 GiB，而检查时可用约 1.4 GiB；这不是 VMware 的永久预算，只是当时的实时状态。三台 VM 各 4 GiB 会让 Windows 与编辑器的余量很小。两节点先完成“代码提交 → 自动测试/构建 → 镜像 → Helm 发布 → 冒烟 → 监控 → 排障/回滚”的主线，更适合当前机器。

本档位为 `nl-cp1`（控制平面，建议 2 vCPU、3 GiB）和 `nl-worker1`（工作节点，建议 2～4 vCPU、5 GiB；压力实测不足再关机升到 6 GiB）。宿主机在开 VM 前应关掉不需要的 Docker Desktop、大型游戏和多余浏览器窗口；两台开齐后 Windows 仍应有足够可用内存，不要把 VM 内存总和等同于宿主机可用内存。

控制平面保留默认 `NoSchedule` 污点，不为了“让两个节点都有业务 Pod”而随意解除。因此业务、MySQL、Redis 和大部分监控 Pod 都在唯一 worker；**两节点集群不具备 worker 故障接管，两个业务副本即使都运行也不能解决这一点**。本档位先用每个无状态服务 1 副本完成闭环，再单独扩一个服务演示滚动升级与 Pod 自愈。三节点扩展时换回默认 values 和 [原 PV 清单](../../deploy/k8s/storage/three-node-local-pv.yaml)，但迁移已有数据库必须先备份并在隔离环境验证恢复，不能原地更改 PV 节点亲和性。

Kubernetes 官方的 kubeadm 前提是每台至少 2 GiB RAM、控制平面至少 2 CPU；这只是集群启动下限，不是承载本项目的容量承诺。[kubeadm 官方前提](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/)

## 第 1 关：集群基础设施确实可用

在 `nl-cp1` 执行，期望两个节点都是 `Ready`，CNI 和 CoreDNS 的 Pod 正常，且 worker 名称确实是 `nl-worker1`：

```bash
kubectl get nodes -o wide
kubectl get nodes --show-labels
kubectl -n kube-system get pods -o wide
kubectl get ingressclass
```

如果 `nl-cp1` 的 `kubeadm init` 仍报内存不足或 `net.ipv4.ip_forward=0`，先修 VM 内存与持久化 sysctl，再重新执行原初始化步骤；**不要使用 `--ignore-preflight-errors` 把真实缺陷藏起来**。如果 worker `NotReady`，先看 `kubectl describe node nl-worker1` 与节点上 `journalctl -u kubelet -n 100 --no-pager`，不要继续装业务。网络策略是否真正生效取决于选用的 CNI；只有 YAML 对象存在不能证明隔离有效。

## 第 2 关：只建立两节点存储

先只读检查已有 PV/PVC，尤其是从三节点方案遗留的同名 `northledger-mysql-local-pv`、`northledger-redis-local-pv`。如果已有对象或数据，请停在这里核对数据与备份；不要直接删除、覆盖或 `kubectl replace --force`。

```bash
kubectl get pv
kubectl -n northledger get pvc 2>/dev/null || true
```

确认是全新、空白的实验集群后，在 `nl-worker1` 建两个**不同目录**：

```bash
sudo install -d -o 999 -g 999 -m 0700 /var/lib/northledger/mysql
sudo install -d -o 999 -g 1000 -m 0700 /var/lib/northledger/redis
sudo stat -c '%U:%G %a %n' /var/lib/northledger/mysql /var/lib/northledger/redis
```

在 `nl-cp1` 的仓库根目录只应用这一份清单：

```bash
kubectl apply -f deploy/k8s/storage/two-node-local-pv.yaml
kubectl get pv,storageclass
```

Local PV 的 `nodeAffinity` 使 Pod 只能调度到持有数据目录的 worker；`Retain` 防止误删 PVC 后数据立即消失，但并不是备份，也不会把数据复制到控制平面。[Kubernetes Local PV 与节点亲和性](https://kubernetes.io/docs/concepts/storage/volumes/#local)

## 第 3 关：先做静态验证，再装业务

复用[三服务发布手册](three-node-microservices-release.md)的 Secret 创建、GHCR 镜像确认和入口准备步骤；其中提到的 worker2 数据目录与三节点 PV 清单，在本档位一律不要执行。`northledger-runtime` 的密码和内部令牌不得进仓库、命令行参数或聊天截图。

在仓库根目录执行：

```bash
helm lint deploy/k8s/helm/northledger-microservices \
  -f deploy/k8s/helm/northledger-microservices/values-two-node-lab.yaml
helm template northledger deploy/k8s/helm/northledger-microservices \
  -n northledger \
  -f deploy/k8s/helm/northledger-microservices/values-two-node-lab.yaml \
  --set-string global.imageTag='<已发布的完整40位提交SHA>' >/dev/null
```

通过后再发布（同样在 `nl-cp1`，并且只使用确实存在的镜像 SHA）：

```bash
helm upgrade --install northledger deploy/k8s/helm/northledger-microservices \
  -n northledger --atomic --wait --timeout 10m \
  -f deploy/k8s/helm/northledger-microservices/values-two-node-lab.yaml \
  --set-string global.imageTag='<已发布的完整40位提交SHA>' \
  --set-string global.version='<已发布的完整40位提交SHA>'
kubectl -n northledger get deploy,sts,pod,pvc,svc,ingress -o wide
```

期望：Access、Ledger、Operations、Web 各 `1/1`；MySQL、Redis 各 1 个 Pod；两个 PVC 都 `Bound` 且其 Pod 在 `nl-worker1`。若有 `Pending`，先看 Pod/PVC Events 和 Local PV 节点亲和性；若有 `CrashLoopBackOff`，先看日志和 Secret/目录权限，不要靠删除 PVC 重试。

## 第 4 关：验证用户路径与受保护发布

运行三服务冒烟脚本时，从**能够访问业务入口且持有测试账户凭据的受保护管理机**执行：

```powershell
./scripts/k8s/smoke-microservices.ps1 -Namespace northledger
```

浏览器不能访问时，先区分入口故障与应用故障：在运行端口转发的同一台机器用 `http://127.0.0.1:18000` 检查；若浏览器在 Windows、转发在 VM，需配置 FinalShell 的 SSH 本地转发或在 Windows 管理机执行 `kubectl port-forward`，不能把 `127.0.0.1` 想成两台机器共用。

GitHub Actions 使用 `two-node-lab` Environment：先在仓库设置审核人、环境 Secret 和仅对集群有必要权限的自托管 Windows runner。默认加载仓库中的 `values-two-node-lab.yaml`；如设置了 `NORTHLEDGER_VALUES_FILE` 覆盖文件，发布前会再次渲染检查四个 `replicas: 1` 和 `global.environment: two-node-lab`，不满足则拒绝发布。首次走 CD 前仍要人工完成 namespace、PV、Secret、Ingress Controller、CNI 等基础设施步骤；流水线不会创建 VM 或初始化 MySQL 数据目录。

## 第 5 关：安装集群内监控并验证告警

按[监控与故障教学手册](three-node-observability-and-incidents.md)安装固定版本的 Prometheus、Alertmanager、Grafana，并启用 node-exporter、kube-state-metrics。重点检查 Access、Ledger、Operations **三个应用指标 Target**、Web Deployment 可用副本与入口冒烟、两台主机的 exporter、Kubernetes 对象指标是否实际有数据。此步骤不能用旧 Docker Compose 页面代替；本档位未配置外部告警接收人，也未持久化监控数据，验收时须明确记录这些边界。

## 第 6 关：留下可追问的证据

至少保存：两个节点 `Ready` 的输出、六个业务工作负载和 PVC 状态、四镜像的同一个 SHA、Helm revision、冒烟结果、Prometheus Targets、一次安全故障演练的 Events/日志/恢复复测。只留 `Running` 截图或一张 Grafana 图不能证明整个业务闭环。具体记录法见[故障教学手册](three-node-observability-and-incidents.md)与[事件模板](../incidents/template.md)。

## VM 创建顺序

等业务/监控配置和本地验证都完成后，再按本项目的[两节点 VMware 创建手册](two-node-vmware-creation.md)准备 `nl-cp1` 与 `nl-worker1`；如果你已经有这两台且未放业务数据，优先核对配置并复用，不为改名或截图而无故重装。
