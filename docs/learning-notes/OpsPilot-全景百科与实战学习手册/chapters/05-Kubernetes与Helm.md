> [返回导航](../README.md) · [打开完整合订本](../OpsPilot-全景百科与实战学习手册.md)

# 第七篇：从单机容器到 Kubernetes

## 第 33 章　为什么有 Compose 还要 Kubernetes

### 33.1 Compose 与 Kubernetes 的边界

Compose 擅长在一台机器上复现多服务；Kubernetes 用控制器持续维护集群中的期望状态，支持多副本、滚动发布、服务发现、资源调度、故障摘流和权限/网络治理。

Kubernetes 不会自动让错误代码变正确，也不会把单副本数据库变高可用。它提高的是运行和交付能力，同时引入更多对象、网络和控制面故障。

### 33.2 从 Docker 对象映射到 K8s

| Docker/Compose | Kubernetes 对应 | 关键差异 |
| --- | --- | --- |
| Container | Container in Pod | Pod 是调度与网络最小单位 |
| service container | Pod + Controller | Pod 可替换，控制器维护副本 |
| Compose service name | Service | 提供稳定虚拟地址和服务发现 |
| port mapping | Service/Ingress/port-forward | 内外入口分层 |
| named volume | PVC/PV/StorageClass | 存储独立于 Pod 生命周期 |
| env/config file | ConfigMap/Secret | 配置对象和敏感对象分离 |
| restart policy | Controller + kubelet | 从单容器重启扩展为期望状态调谐 |

### 33.3 声明式与控制循环

你提交 Deployment 描述“需要两个 API Pod”。Kubernetes 控制器不断比较期望状态与实际状态：少一个就创建，多一个就处理，模板变化就滚动替换。

`kubectl apply` 成功只代表 API Server 接受对象，不代表 Pod 已 Ready；后续还可能拉镜像失败、调度失败、探针失败或配置错误。因此必须继续看 rollout、Pod、Events 和日志。

## 第 34 章　Pod、Deployment、ReplicaSet：应用怎样自愈

### 34.1 Pod

Pod 包含一个或多个共享网络命名空间和部分存储的容器。OpsPilot API Pod 含一个 initContainer 和一个 API 容器；Web Pod 含 Nginx 容器。

Pod 名和 IP 是可变的。不能把 Pod IP 写入前端；由 Service 选择 Ready Pod。

### 34.2 initContainer

API 的 `wait-for-database` 使用 API 镜像内的 `nc` 循环检查数据库 TCP 端口，成功后主容器才启动。它减少首次并行创建时 Flyway 无意义失败。

边界：端口开放不代表数据库用户可认证或 schema 正确。真正连接仍由应用完成。若数据库永久不可达，initContainer 会持续等待，需要从其日志、DNS、Service、NetworkPolicy 和数据库 Pod 排查。

### 34.3 Deployment 与 ReplicaSet

API 和 Web 是 Deployment，默认各两副本。Deployment 管理 ReplicaSet，ReplicaSet 管理 Pod。删除一个受控 Pod，控制器创建新 Pod 恢复副本，这是自愈演示。

不要把“Pod 重启”与“Pod 被替换”混为一谈：容器重启可能保持 Pod 名，控制器重建会产生新 Pod UID/名称和 IP。

### 34.4 滚动更新

`maxUnavailable=0`、`maxSurge=1` 表示升级时不主动降低可用副本，最多额外创建一个新 Pod，等新 Pod Ready 再替换旧 Pod。

它需要额外容量；资源不足时新 Pod 可能 Pending，rollout 卡住。readiness 错误可能让新版本永远不进入 Service，保护旧容量；liveness 错误可能形成重启循环。

### 34.5 revisionHistoryLimit

Deployment 保留 5 个 ReplicaSet 修订用于工作负载级回滚。Helm 还保存整个 Release 的多资源历史。二者层级不同：Deployment revision 管单个工作负载模板，Helm revision 管 Chart 渲染出的整套资源。

## 第 35 章　Service、EndpointSlice 与 Ingress

### 35.1 Service

`opspilot-backend` ClusterIP Service 通过标签选择 API Pod，在 18080 提供稳定入口；`opspilot-web` 选择 Web Pod，在 8080 提供入口。

Service 没有可用后端时仍可能存在 ClusterIP。要看 EndpointSlice 是否包含 Ready Pod 地址。

```bash
kubectl -n opspilot get svc
kubectl -n opspilot get endpointslice
kubectl -n opspilot describe svc opspilot-backend
```

### 35.2 标签和选择器

Service/Deployment/NetworkPolicy 都依赖标签。selector 拼错时 Pod 可能健康但 Service 无端点。排障要对比 Pod labels 与 Service selector，而不是只重启。

### 35.3 Ingress

Ingress 声明域名/路径到 Service 的 HTTP 路由，但它必须有可工作的 Ingress Controller。只有 Ingress 对象，没有 Controller，不会产生实际流量入口。

本地 Chart 有 `opspilot.local` 模板，并通过 lint 和服务端 dry-run；历史实机因 Ingress Controller 镜像与残留 webhook 问题，Release 使用 `ingress.enabled=false`，通过 port-forward 验收。因此不能说“本机 Ingress 已实际跑通”。

### 35.4 port-forward

```powershell
kubectl -n opspilot port-forward service/opspilot-web 18000:8080
```

它在本机建立临时转发，适合调试和验收；终端关闭即停止，不是生产入口，不具备负载均衡器、域名、TLS 和高可用能力。

## 第 36 章　ConfigMap、Secret 与配置变更

### 36.1 为什么分离

ConfigMap 保存环境、版本、DB 地址、用户名等非敏感配置；Secret 保存数据库密码。分离便于权限和生命周期控制。

Kubernetes Secret 默认只是 base64 编码，不是自动加密。生产需要 etcd 静态加密、RBAC、外部密钥平台、轮换和审计。

### 36.2 envFrom 与 secretKeyRef

API 从 ConfigMap 批量导入环境变量，DB_PASSWORD 从 Secret 单键读取。工作负载不挂载 Kubernetes API Token，减少不必要权限。

### 36.3 配置更新为什么触发滚动

Pod template annotations 包含 ConfigMap/Secret 渲染内容的校验和。配置变化时 template hash 变化，Deployment 创建新 ReplicaSet。否则只修改 ConfigMap，已有以环境变量启动的 Pod 不会自动重读。

### 36.4 生产 Secret 模式

默认 values 创建本地 Secret，并含演示密码；production example 设置 `create=false`，引用由 Vault/云密钥/平台团队预先创建的 existingSecret。

不能把明文演示 values 当生产密钥管理。Git 历史中的秘密即使后续删除也可能残留，必须轮换。

## 第 37 章　StatefulSet、PVC 与 MySQL 边界

### 37.1 为什么 MySQL 不用 Deployment

StatefulSet 为 Pod 提供稳定序号和与实例绑定的卷。MySQL 为单副本 `mysql-0`，通过 Headless Service 获得稳定网络身份，数据写 5Gi PVC。

Pod 重建后重新挂载 PVC，证明数据生命周期独立于 Pod。

### 37.2 PV、PVC、StorageClass

PVC 是应用对存储的请求；PV 是实际存储资源；StorageClass 定义动态供应方式。Minikube 默认存储可让 5Gi PVC Bound。

`ReadWriteOnce` 表示通常单节点读写挂载，不等于只能一个 Pod 读，也要结合驱动语义。

### 37.3 单副本不是高可用

PVC 能防 Pod 重建丢数据，却防不了节点/磁盘故障、数据库进程长期不可用、误操作或区域灾难。一个 StatefulSet Pod 不等于复制、自动故障转移和备份恢复。

类生产 values 关闭内置 MySQL，连接企业托管/高可用数据库。若自建，应采用成熟 Operator 或受控主从/集群方案，并验证备份、恢复、RPO/RTO、故障转移和一致性。

### 37.4 删除资源的风险

删除 StatefulSet 不一定删除 PVC；删除 Helm Release 对 PVC 的行为需看资源策略；手工删 PVC 可能导致数据不可恢复。任何清理前先列出并确认备份，不把卸载命令写成无脑一键删除。

## 第 38 章　三类探针与优雅摘流

### 38.1 API 探针

startup 每 5 秒，最多 30 次失败，为启动留出约 150 秒；readiness 每 5 秒，连续 3 次失败摘流；liveness 每 10 秒，连续 3 次失败重启。

API preStop 先 sleep 5 秒，给 EndpointSlice 摘流传播，再由终止信号触发 Spring Boot 优雅停机。terminationGracePeriod 35 秒，大于 Spring 20 秒停机窗口。

### 38.2 Web 探针

startup/readiness 访问 `/health`，覆盖 Nginx 到 API；liveness 访问 `/`，只验证 Nginx 静态服务。这样后端暂时失败时 Web Pod 会不就绪，但不会因为共享后端问题不停重启 Nginx。

### 38.3 探针设计风险

- liveness 依赖共享数据库：数据库故障导致所有 Pod 重启风暴。
- readiness 过严：所有实例同时摘流，用户完全不可用。
- timeout 太小：高负载时健康请求本身超时。
- 路径与业务入口不同：探针绿但真实链路坏。
- 同一重型检查被高频调用：健康检查反而制造负载。

当前 readiness 数据库包含关系需要实测和配置澄清，不能只依赖代码注释。

## 第 39 章　requests、limits 与 HPA

### 39.1 requests 和 limits

request 用于调度和 HPA CPU 利用率基准；limit 限制最大资源。API request 150m/256Mi，limit 1 CPU/768Mi；Web 更小；MySQL request 200m/512Mi，limit 1 CPU/1Gi。

CPU 超过 limit 会被节流；内存超过 limit 可能 OOMKilled。request 过低会让节点过度承诺，过高会使 Pod Pending。值应由测量调整。

### 39.2 HPA

API HPA 目标 2 至 4 副本，平均 CPU 70%。扩容窗口短、缩容稳定 300 秒，减少抖动。

CPU 利用率通常相对 request 计算；request 不合理会影响 HPA。HPA 还依赖 Metrics Server。历史本机 Metrics Server 镜像未成功，因此 HPA `TARGETS=unknown`，只能说对象和策略存在，不能说实际触发扩容。

### 39.3 本地扩容实验条件

先确认 `kubectl top pods` 有数据；再使用受控压测产生持续 CPU；观察 HPA target、desired replicas、Deployment/Pod 变化和服务延迟；停止负载后等待缩容窗口。无 Metrics Server 时不要制造压力却声称 HPA 工作。

## 第 40 章　PDB、拓扑与计划中断

### 40.1 PDB

API/Web PDB `minAvailable=1`，在 voluntary disruption（如 drain）时阻止同时驱逐所有副本。它不防节点突然宕机、容器崩溃，也不主动创建副本。

Minikube 单节点时 PDB 无法证明跨节点维护保护；可能反而让 drain 卡住。真实价值需要多节点和正确副本分布。

### 40.2 topologySpreadConstraints

配置尝试按 hostname 均匀分布副本，`ScheduleAnyway` 是软约束。单节点环境所有 Pod 仍在同一节点，所以两副本不等于节点高可用。

## 第 41 章　RBAC 与 ServiceAccount

### 41.1 工作负载身份

API/Web/MySQL 使用各自 ServiceAccount，但 `automountServiceAccountToken=false`，因为业务不需要调用 Kubernetes API。避免 Token 进入容器能缩小泄露面。

### 41.2 operator 最小权限

单独 `opspilot-operator` 可 get/list/watch Pods、日志、Service、Endpoint、Event、ConfigMap 和工作负载状态，并可 patch Deployment 用于受控 restart；不能读 Secret、删除 Pod或修改数据库 StatefulSet。

历史验收验证了可读 Pod、不可读 Secret、不可删除 Pod。RBAC 不是“给运维 cluster-admin 才方便”，最小权限是企业环境基本原则。

### 41.3 验证

```bash
kubectl auth can-i get pods --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
kubectl auth can-i get secrets --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
kubectl auth can-i delete pods --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
```

只检查 Role YAML 不如让 API Server 实际回答权限。

## 第 42 章　NetworkPolicy

### 42.1 当前策略

先对带 OpsPilot 标签的 Pod 默认拒绝入站，再放行：外部到 Web 8080、Web 到 API 18080、API 到 MySQL 3306。

它把架构图转化为网络最小权限：Web 不应直连数据库，其他 Pod 不应绕过 Web 随便调用 API。

### 42.2 CNI 边界

NetworkPolicy 只有在集群网络插件支持并执行时才有效。对象创建成功不等于规则真正被执行。Minikube 驱动/CNI 模式必须确认。

当前只设置 Ingress 策略，没有默认拒绝 Egress。生产还需考虑 DNS、外部数据库、监控抓取、Ingress Controller namespace、备份和必要出口，不能简单全拒绝后让应用失联。

### 42.3 策略排障

Service 有端点但连接超时，检查源/目标 Pod 标签、namespaceSelector/podSelector 组合、端口和 CNI。临时删除策略会扩大网络权限，不应在生产无审批操作；先用测试 Pod和策略描述收集证据。

## 第 43 章　Helm：参数化安装、升级和回滚

### 43.1 Chart 结构

`Chart.yaml` 描述名称和版本；`values.yaml` 是本地默认；production example 演示外部数据库与镜像仓库；templates 根据 values 生成 K8s 对象；NOTES 给出安装后提示；tests 含 Release Test Pod。

### 43.2 模板与 values

同一模板通过 values 改镜像标签、副本、资源、Ingress、数据库模式和 Secret。参数化避免复制多套 YAML 漂移，但模板复杂后必须 lint、render、server dry-run 和实装验证。

### 43.3 lint、template、dry-run

- `helm lint` 检查 Chart 结构和部分模板问题。
- `helm template` 本地渲染，不访问集群。
- Kubernetes server dry-run 让 API Server 验证资源 schema/准入。
- 真正 install/upgrade 才验证调度、镜像、探针、网络和运行。

它们是逐层门禁，不能用 lint 通过代替实机运行。

### 43.4 Release 与 revision

Helm Release `opspilot` 安装在 namespace `opspilot`。每次成功/失败升级形成 revision。`helm history` 显示 deployed、superseded、failed 等状态；回滚脚本选择最近可用历史，等待 API/Web rollout。

数据库 schema 变化可能使应用回滚不兼容，因此“Helm 能回滚”不等于任何版本都能安全回滚。迁移应尽量向前/向后兼容，破坏性变更分阶段。

### 43.5 Helm test

测试 Pod 使用 Web 镜像访问 `opspilot-web:8080/health`，经过 Nginx 到 API readiness。成功后清理，失败时保留日志。

它验证集群内入口健康链，但不是完整业务。另有 smoke-test 脚本创建账户、转账、重放和流水平衡，二者互补。

## 第 44 章　Minikube 一键部署脚本逐步拆解

### 44.1 前置检查

脚本检查 docker、minikube、kubectl、helm、mvn、npm；启动或复用 Docker driver Minikube；可选启用 ingress 和 metrics-server。

机器资源要同时容纳 Minikube VM/容器、API/Web/MySQL、插件和宿主工具。资源不足会出现 Pod Pending、镜像构建慢或系统卡顿。历史脚本默认曾使用 6GiB，学习规划也考虑过 4GiB约束，实际执行以当前脚本和机器可用资源为准。

### 44.2 构建和加载镜像

先生成 JAR/dist，构建 API/Web runtime 镜像，再用 `minikube image load` 加载 API、Web 和 MySQL，减少集群拉取公网失败。

本地镜像更新后必须重新 load；标签相同且 `IfNotPresent` 时可能仍用旧镜像，因此推荐使用新版本标签或明确检查镜像 ID。

### 44.3 Helm 部署门禁

脚本使用 upgrade/install、等待和失败回滚；随后分别等待 API/Web rollout、执行 Helm test、列出资源。正常结束不只看 Helm 命令返回 0。

### 44.4 降级路径

镜像网络或 Ingress webhook 异常时可 `-SkipAddons -DisableIngress`，保留核心 API/Web/MySQL/Service，通过 port-forward 验收。降级路径必须在简历和演示中说清，不应删除集群安全 webhook 来伪造成功。

## 第 45 章　Kubernetes 排障百科

### 45.1 通用顺序

```text
资源状态 -> Events -> describe -> 容器/initContainer 日志
-> Deployment/ReplicaSet -> Service/EndpointSlice
-> 探针 -> ConfigMap/Secret 引用 -> NetworkPolicy
-> 节点资源/存储 -> 恢复与复盘
```

### 45.2 Pending

Pod 未调度/未运行。看 Events：资源不足、PVC 未绑定、节点选择/污点、镜像拉取前置等。Pending 不等于应用代码错误。

### 45.3 ImagePullBackOff

先 `describe pod` 和 Events，区分镜像不存在、认证、DNS、代理和连接失败。自研镜像可重新 build/load；私有仓库用 imagePullSecret；不要反复删 Pod，控制器只会再次遇到同一问题。

### 45.4 CrashLoopBackOff

容器启动后反复退出。看当前和 previous logs、退出码、OOMKilled、环境、Flyway、权限、只读文件系统。`kubectl logs --previous` 常能看到上一轮崩溃根因。

### 45.5 Init:0/1 或 Init:CrashLoopBackOff

查看 initContainer 日志。OpsPilot 可能在等待数据库 DNS/端口；检查 mysql Service、Pod、Endpoint、NetworkPolicy 和密码初始化，而不是只查 API 主容器。

### 45.6 Running 但 0/1 Ready

说明主进程运行但 readiness 失败。describe 看 probe 错误，Pod 内/Service 路径 curl，检查应用可用状态。不要用删除探针作为永久修复。

### 45.7 Service 无法访问

看 Service selector、EndpointSlice、Pod readiness、targetPort 名、NetworkPolicy。ClusterIP 只在集群内，宿主浏览器不能直接访问。

### 45.8 Ingress 404/502

404 可能 Host/path 不匹配或 Controller 默认后端；502 可能 Service/Endpoint/端口/NetworkPolicy；还要看 IngressClass、Controller Pod、Events 和 webhook。修改本机 hosts 只解决域名解析，不会安装 Controller。

### 45.9 PVC Pending

查看 StorageClass、provisioner、容量和 access mode。删除 Pod不会解决 StorageClass 缺失。Minikube 重建后本地存储可能变化，重要数据不能只依赖实验集群。

### 45.10 HPA unknown

检查 Metrics Server、APIService、`kubectl top`、Pod requests 和 HPA events。没有指标时 HPA 不能计算，双副本仍由 Deployment 保持。

### 45.11 Rollout 卡住

看新 ReplicaSet Pod 的 Pending、pull、probe 和资源；旧 Pod是否因 maxUnavailable=0 保留；PDB主要影响驱逐，不要混淆。发布门禁超时后根据影响回滚，并保留失败 Pod/日志。

## 第 46 章　Kubernetes 实验与个人掌握标准

### 46.1 必做实验

1. 从零部署一次，不只运行脚本，记录每阶段对象。
2. 删除一个 API Pod，观察新名称/IP与副本恢复，验证 Service 地址不变。
3. 查看 initContainer Completed 和日志。
4. 修改镜像版本执行滚动升级，观察旧/新 ReplicaSet。
5. 执行 Helm history 和一次回滚。
6. 运行业务 smoke-test，证明账户、转账、重放和流水。
7. 验证 operator 能/不能做的三项权限。
8. 观察 PVC 与 MySQL Pod 重建后的数据。
9. 在有 Metrics Server 时再做 HPA 实验；无则记录 unknown 边界。

### 46.2 复述模板

“Compose 用于单机复现，Kubernetes 维护多副本期望状态。OpsPilot 的 API/Web 用 Deployment，各两副本，通过 Service 稳定发现；滚动策略 maxUnavailable=0/maxSurge=1，startup/readiness/liveness 区分启动、摘流和重启。MySQL StatefulSet+PVC 只用于学习持久化，不是生产高可用；类生产 values 关闭内置库并连接外部数据库。HPA、PDB、NetworkPolicy 的效果依赖 Metrics Server、多节点和支持策略的 CNI，所以我会分别说明对象存在、实机验证和生产边界。”

### 46.3 本篇验收题

1. Pod 被删后为什么回来？谁创建的？
2. Service 为什么比 Pod IP 稳定？
3. Ingress 对象和 Ingress Controller 有什么区别？
4. readiness 失败与 liveness 失败分别发生什么？
5. initContainer 等到 3306 为什么仍不能证明数据库可用？
6. 两副本在单节点为什么不是节点高可用？
7. PVC 与备份有何区别？
8. HPA 为什么依赖 request 和 Metrics Server？
9. PDB 为什么防不了节点突然宕机？
10. NetworkPolicy 对象存在为什么不一定真正生效？
11. Helm test 与业务 smoke-test 各证明什么？
12. 数据库迁移为什么会限制应用回滚？
