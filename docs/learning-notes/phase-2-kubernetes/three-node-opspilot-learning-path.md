# 三节点 Kubernetes × OpsPilot 系统学习路线

## 1. 现在处于什么阶段

截至 2026-08-18，已经能够从 FinalShell 管理一个由 `k8s-master`、`k8s-node1`、`k8s-node2` 组成的集群，并在 master 上实际完成过 Deployment 扩缩容、镜像滚动更新、查看发布历史和版本回滚。

这说明当前不必再从“什么是 Pod”重新听一遍，而应该把已经做过的命令放回完整工程链路中理解：

```text
Java 源码
  → Maven 测试与 JAR
  → Docker 镜像
  → 镜像分发
  → Kubernetes Deployment / StatefulSet
  → Service / Ingress
  → 探针、滚动发布与回滚
  → 指标、告警、日志
  → 故障演练、恢复与复盘
```

这里不把“执行过命令”当作“已经熟练”。当前能力假设是：Kubernetes 已有基础实操但需要系统串联，Docker 刚入门，Java/前端有知识基础但很久没有独立写完整项目。因此每次 Kubernetes 改造都同时安排对应的 Java 请求链阅读、Maven 测试、Docker 构建和故障定位，逐步恢复项目开发手感。

## 2. 为什么 OpsPilot 正适合这套集群

OpsPilot 不是为了凑 Kubernetes 对象而临时写的 Hello World。它已经有三段真实请求链路：

```text
浏览器
  → React + Nginx Web
  → Spring Boot API
  → MySQL
```

它还已经具备 Dockerfile、健康端点、Prometheus 指标、Helm Chart、滚动发布、PDB、HPA、RBAC 和 NetworkPolicy 骨架。三台虚拟机让我们能把单节点 Minikube 无法真实观察的内容补上：

- API 和 Web 副本是否真的分散到 `node1`、`node2`。
- 一个工作节点故障时，Pod 怎样重新调度。
- Service 如何把请求转给不同节点上的 Pod。
- 本地磁盘型 PVC 为什么会限制数据库迁移。
- PDB 能保护“主动维护”，为什么不能阻止机器突然断电。
- 单 master + 双 worker 能提高业务工作负载的可用性，但控制平面仍是单点。

## 3. 项目与 Kubernetes 对象的一一对应

| OpsPilot 内容 | Kubernetes 对象 | 学习重点 |
| --- | --- | --- |
| Spring Boot API | Deployment + ClusterIP Service | 副本、自愈、探针、滚动发布 |
| React/Nginx Web | Deployment + ClusterIP Service | 反向代理、服务发现、无状态扩容 |
| MySQL 8.4 | StatefulSet + Headless Service + PVC | 稳定身份与数据持久化；不冒充数据库高可用 |
| `application.yml` 的公开配置 | ConfigMap | 同一镜像适配不同环境 |
| 数据库密码 | Secret | 不把真实密码提交到 Git |
| 外部访问 | Ingress；控制器缺失时先用 NodePort/port-forward 验证 | 七层路由不是 Ingress 对象自动完成的 |
| CPU 扩缩容 | Metrics Server + HPA | requests 是调度与 HPA 的基础 |
| 节点维护 | 多副本 + topology spread + PDB | 副本分布和可驱逐预算必须同时存在 |
| 指标与告警 | Prometheus + Grafana + Alertmanager | 从“Pod Running”升级到业务可观测 |

## 4. 部署前的五道门禁

当前 Helm Chart 在 `Chart.yaml` 中声明 Kubernetes 必须 `>=1.27.0`。因此在不知道集群版本时，不能直接执行 `helm install`。

### 门禁 A：Kubernetes 版本

- `>= 1.27`：进入当前 Helm 主线。
- `1.23～1.26`：先检查 API 兼容性并做服务端 dry-run，再决定是否调整 Chart 支持范围。
- `<= 1.22`：保留为旧版教程实验集群；先用最小工作负载学习，不把当前完整 Chart 强行装进去。

### 门禁 B：容器运行时

“每台机器装了 Docker”不等于“Kubernetes 正在用 Docker 运行 Pod”。必须查看节点的 `containerRuntimeVersion`。Docker、containerd 的本地镜像导入方式不同；确认前不写镜像分发脚本。

### 门禁 C：CNI 网络插件

NetworkPolicy 对象能创建，不代表策略一定生效。只有网络插件支持并实施策略时，默认拒绝和逐段放行才有真实效果。

### 门禁 D：StorageClass

MySQL 的 PVC 需要动态制备器或预先创建的 PV。若没有默认 StorageClass，PVC 会一直 Pending。三节点环境中的本地卷还会把 MySQL 绑定在某个节点上，它只用于学习持久化，不是高可用数据库。

### 门禁 E：Ingress Controller

只有 Ingress 对象、没有 Ingress Controller 时，外部流量不会自动进入 Service。第一阶段可以用 port-forward 或 NodePort验收；确认控制器后再走域名入口。

## 5. 学习方法：每一课都走六步

每一阶段都按照“原理 → 观察 → 操作 → 故障 → 复述 → 验收”推进。老师不会一次性把所有 YAML 交给学习者执行；每完成一小步，先读输出并解释，再进入下一步。

### 阶段 0：集群体检与架构地图

- 原理：理解 control plane、kubelet、容器运行时、CNI、CSI/StorageClass 的分工。
- 观察：运行只读体检，记录版本、节点 IP、运行时、系统 Pod、存储和入口能力。
- 操作：只整理事实，不修改集群。
- 故障：识别 NotReady、系统 Pod 非 Running、Metrics API 缺失、PVC 无制备器等风险。
- 复述：解释“Docker 已安装”和“Kubernetes 的 CRI 运行时”为什么是两件事。
- 验收：能画出 master、node1、node2 以及 Pod/Service 的关系图。

### 阶段 1：从 Compose 映射到 Kubernetes

- 原理：Compose 管理单机容器，Kubernetes 控制器维护跨节点期望状态。
- 观察：对照 `compose.yml` 和 Helm templates，找到 Web、API、MySQL 的配置来源。
- 操作：先渲染 Chart，不安装；逐个读 Deployment、Service、StatefulSet。
- 故障：故意把 Service selector 与 Pod label 对不上，在渲染文件中定位问题。
- 复述：说清 Deployment、ReplicaSet、Pod 的所有与被所有关系。
- 验收：能从某个 Pod 反查 ReplicaSet 和 Deployment。

### 阶段 2：构建和分发 OpsPilot 镜像

- 原理：Kubernetes 调度 Pod，但不会把开发机本地镜像自动复制到两台 worker。
- 观察：确认 API/Web 镜像标签、节点运行时和仓库网络可达性。
- 操作：执行 Maven 测试，构建不可变版本标签，再按审计结果选择局域网 Registry 或受控导入。
- 故障：制造一次不存在的镜像标签，沿 Events → Pod describe → 节点运行时定位 `ImagePullBackOff`。
- 复述：解释为什么 `latest` 不适合作为可回滚版本。
- 验收：两台 worker 都能取得同一摘要的镜像，且不会靠每台机器手工重新 build。

### 阶段 3：先上无状态 Web/API

- 原理：Deployment 适合可替换的无状态副本，Service 提供稳定访问入口。
- 观察：查看调度结果、Pod IP、EndpointSlice 和探针状态。
- 操作：部署两个 API、两个 Web 副本，使它们尽量分布到 node1/node2。
- 故障：删除一个 API Pod，观察控制器补回；再让 readiness 失败，观察 Service 摘流。
- 复述：解释“容器重启”和“Pod 被重新创建”的区别。
- 验收：删除 Pod 后副本数恢复，Service 地址不变，业务请求继续可用。

### 阶段 4：接入 MySQL 与持久化

- 原理：StatefulSet 提供稳定身份；PVC 申请存储；Flyway 管理数据库结构。
- 观察：查看 PVC Bound 状态、PV 位置、MySQL Pod 所在节点和 Flyway 日志。
- 操作：部署学习用单副本 MySQL，创建账户和转账，再重建 Pod。
- 故障：观察无 StorageClass 时的 Pending，或节点不可用时本地卷无法迁移的边界。
- 复述：解释“PVC 还在”为什么不等于“已有备份”。
- 验收：Pod 重建后业务数据仍在；同时明确它不是生产 MySQL HA。

### 阶段 5：入口、更新和节点维护

- 原理：Ingress 管理七层路由；Deployment 管理版本；PDB 只限制主动驱逐。
- 观察：查看 IngressClass、Controller、ReplicaSet revision 和 Pod 节点分布。
- 操作：发布新版本、观察滚动过程、做 Helm 回滚。
- 故障：在确认副本分布和 PDB 后，受控执行一次 worker 节点维护演练。
- 复述：比较 `rollout undo` 与 `helm rollback` 的作用范围。
- 验收：升级期间旧版本保留容量；回滚后镜像标签、应用版本和业务请求同时验证通过。

### 阶段 6：监控、日志与告警

- 原理：Running 只表示容器状态，不表示转账业务正确。
- 观察：采集 JVM、HTTP、数据库连接和 Kubernetes 对象指标。
- 操作：接入 Prometheus/Grafana/Alertmanager；资源不足时把 Loki/Alloy 留作可选项。
- 故障：制造 API readiness 失败或高错误率，验证告警触发和恢复。
- 复述：解释指标、日志、事件各自能回答什么问题。
- 验收：用一个真实请求串起浏览器、Ingress/Web、API、MySQL 和监控证据。

### 阶段 7：综合故障演练与面试证据

- 原理：项目价值来自可重复验证的恢复能力，不来自 YAML 数量。
- 观察：保存节点、工作负载、事件、发布历史、监控和业务数据证据。
- 操作：完成 Pod 删除、错误镜像、节点维护、应用回滚、数据库备份恢复五类演练。
- 故障：每次只改变一个变量，按资源状态 → Events → 日志 → Service/EndpointSlice → 探针顺序定位。
- 复述：用 5 分钟讲清架构、一次故障、证据和生产边界。
- 验收：形成 Runbook、演练记录和保守的简历表述。

## 6. 今天的第一项操作：只读体检

脚本位于：

```text
D:\Develop\OpsPilot\scripts\k8s\audit-three-node-cluster.sh
```

在 FinalShell 中把它上传到 master 的 `/root/opspilot-lab/`，然后执行：

```bash
cd /root/opspilot-lab
chmod +x audit-three-node-cluster.sh
./audit-three-node-cluster.sh | tee cluster-audit-2026-08-18.txt
```

预期结果不是“所有命令都必须成功”。例如没有 Helm、没有 Metrics Server 或没有 IngressClass 都是允许的学习环境事实。验收要求是：保存完整输出，并能从中指出五道门禁各自的当前状态。

## 7. 暂时不要做的操作

在体检结果分析完成前，不执行以下操作：

- 不执行当前 OpsPilot Chart 的 `helm install`。
- 不关闭防火墙或 SELinux 来“碰碰运气”。
- 不给 master 去掉污点来强行塞业务 Pod。
- 不在每个节点分别重新构建同名镜像。
- 不创建来路不明的默认 StorageClass。
- 不先开启 NetworkPolicy 再猜网络为何断开。

这些能力后面都会练，但每次都要先有观察基线和明确验收条件。

## 8. 官方概念依据

- Kubernetes Service 为会被替换的 Pod 提供稳定访问入口：<https://kubernetes.io/docs/concepts/services-networking/service/>
- Ingress 只有在控制器存在时才真正工作：<https://kubernetes.io/docs/concepts/services-networking/ingress/>
- Deployment 适合无状态工作负载，StatefulSet 适合需要稳定状态的工作负载：<https://kubernetes.io/docs/concepts/workloads/>
- startup probe 成功前不会启动 liveness/readiness 检查：<https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/>
- StatefulSet 的存储需要动态制备或预先创建：<https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/>
- NetworkPolicy 是否生效取决于网络插件支持：<https://kubernetes.io/docs/concepts/services-networking/network-policies/>
