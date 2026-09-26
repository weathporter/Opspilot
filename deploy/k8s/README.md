# OpsPilot 旧单体 Kubernetes 交付（兼容路径）

当前微服务主线已迁至 [NorthLedger 两节点发布与验收手册](../../docs/runbooks/two-node-microservices-release.md) 和 `deploy/k8s/helm/northledger-microservices`；资源允许时可按[三节点扩展手册](../../docs/runbooks/three-node-microservices-release.md)实验。以下保留原单体 Minikube/旧实验环境的历史操作，不应与新三服务的资源名、Secret、数据目录或 CI 发布流程混用。

这里不是“放几张 YAML 就算上了 K8s”，而是一套可以安装、升级、回滚、冒烟验证和解释生产边界的 Helm 交付。

使用此前搭建的 `Kubernetes v1.21 + Docker + Calico` 三节点虚拟机时，不直接安装要求 `>=1.27` 的 Helm Chart；请使用[三节点虚拟机部署实战](vm-lab/README.md)。该目录是旧版本教学兼容层，现代交付主线仍是本目录下的 Helm Chart。

## 它解决什么

Compose 适合单机复现；Kubernetes 负责多副本期望状态、滚动升级、故障摘流、资源治理和权限边界。两套部署都运行同一组 API/Web 镜像，差异只在运行编排层。

```text
用户
  → Ingress
  → opspilot-web Service
  → React/Nginx Pod × 2
  → opspilot-backend Service
  → Spring Boot Pod × 2～4
  ├─ MySQL StatefulSet + PVC（本地学习）
  └─ Redis StatefulSet + PVC + Redis Exporter（本地学习）
     生产覆盖改用外部高可用 MySQL 与 Redis
```

## 已实现资源

| 能力 | 实现 | 为什么 |
| --- | --- | --- |
| 滚动发布 | API/Web Deployment，maxUnavailable=0 | 新版本未就绪时保留旧版本容量 |
| 自愈与摘流 | startup/readiness/liveness | 区分启动慢、暂不可接流量和进程死亡 |
| 弹性 | HPA 2～4，CPU 70% | 展示基于资源指标的水平扩缩容 |
| 计划中断保护 | API/Web PDB，minAvailable=1 | 节点维护时避免同时驱逐全部副本 |
| 资源治理 | requests/limits | 提供调度依据并限制异常资源消耗 |
| 容器安全 | 非 root、只读根文件系统、drop ALL、seccomp | 降低容器逃逸和持久化写入风险 |
| 网络边界 | 默认拒绝入站，Web→API→MySQL/Redis 与 Prometheus→Exporter 逐段放行 | 把调用链落实为最小网络权限 |
| 权限边界 | 工作负载不挂 Token；operator Role 不读 Secret | 避免业务 Pod 获得无用集群权限 |
| 启动时序 | API initContainer 等待 MySQL 与 Redis TCP 就绪 | 避免 Flyway/Session 在依赖初始化期失败并触发无意义重启 |
| 数据持久化 | MySQL/Redis StatefulSet + PVC | 学习 Pod 重建后业务数据、AOF 和会话状态保留 |
| Redis 可观测性 | Redis Exporter Deployment/Service | 让平台 Prometheus 抓取连接、内存、命中和淘汰指标 |
| 配置密钥 | ConfigMap 与 Secret 分离 | 公开配置与敏感值使用不同生命周期 |
| 发布验收 | Helm test 访问 Web Service `/health` | 在集群内验证 Nginx→API→MySQL/Redis readiness 链路 |

## 本机部署

前提：Docker Desktop、Minikube、kubectl、Helm。当前项目使用 Docker driver。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1
```

脚本会检查每个外部命令的退出状态，启动或复用 Minikube，构建 API/Web 运行时镜像，并在本机缺失依赖镜像时先拉取固定标签，再通过一次性 Docker archive 显式覆盖加载 API、Web、MySQL、Redis、Redis Exporter 和 BusyBox；archive 加载后立即删除，避免 BuildKit manifest list 与节点同标签缓存让旧镜像继续运行。它从进程环境或已忽略的本机 `.env` 读取运行密钥，通过 stdin 创建 `opspilot-runtime` Secret，Helm values 和命令行不出现明文密码；随后按实际版本选用 Helm 3 的 `--atomic` 或 Helm 4 的 `--rollback-on-failure` 执行失败自动回滚。开发环境重复使用同一版本标签时，脚本还会主动滚动 API/Web，避免 Helm 因 Pod 模板未变化而继续运行旧进程；最后验证 API/Web/Redis/Exporter rollout、Release Test 和资源状态。

镜像网络受限，或已有集群残留失效 Ingress Webhook 时，不应删除集群级安全配置；使用本地降级参数：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1 -SkipAddons -DisableIngress
```

此时通过 Service 端口转发验收。HPA 对象仍会创建，但没有 Metrics Server 时 `TARGETS` 显示 `unknown`，不能声称已经触发自动扩容。

端口转发：

```powershell
kubectl -n opspilot port-forward service/opspilot-web 18000:8080
```

浏览器访问 <http://localhost:18000>。端到端冒烟：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/smoke-test.ps1
```

它会真实创建账户、完成转账、用相同幂等键重放，并检查两条流水和 `balanced=true`。
同时，它会验证管理员会话确实写入 Redis、运维总览缓存拥有有限 TTL，再提交一次账户写入并证明缓存键已按事务提交事件删除、下次读取可重新生成，最后主动登出清理会话。

已有 MySQL PVC 时，数据库内账号密码仍是首次初始化值。部署脚本会优先复用与该 PVC
匹配的 `opspilot-database`（旧版）或 `opspilot-runtime`（当前版）Secret；如果数据卷存在但
匹配 Secret 丢失，脚本会拒绝升级，避免“只改 Secret、数据库密码未变”导致新 Pod 全部不可用。
同样，已有 Redis PVC 时脚本会复用 `opspilot-runtime` 中的当前密码，不会把 `.env`
中新值当成隐式轮换。如果确实要轮换本地内置 Redis，必须按 Runbook 协调更新
`opspilot-runtime`、Redis StatefulSet、API Deployment 和 Redis Exporter。类生产外部 Redis 使用
`secrets.existingSecret` 指定的对象（示例为 `opspilot-production-runtime`）且没有内置 StatefulSet；
应先按企业中间件平台流程轮换服务端，再只滚动 API 与 Redis Exporter。两条流程都必须
验证登录、readiness、缓存 TTL 与监控指标，不可混用资源名。

## 发布与回滚

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1 -Version 0.3.0
powershell -ExecutionPolicy Bypass -File scripts/k8s/rollback.ps1
```

Helm 保存应用发布历史，Deployment 保留 5 个 ReplicaSet revision；前者负责应用级回滚，后者便于单工作负载排障。

## MySQL 与 Redis 的生产边界

默认单副本 MySQL 与 Redis StatefulSet 都是学习环境，不是生产高可用方案。它们用于证明稳定身份、PVC、Secret、探针、资源限制、服务发现和 NetworkPolicy。Redis AOF 能覆盖普通 Pod 重建，但不能替代跨节点高可用与受控备份。

类生产部署使用 `values-production.example.yaml`，关闭内置 MySQL/Redis，连接企业托管高可用服务，并要求外部密钥系统预建 Secret。示例启用 `rediss://`，Spring Boot 通过只读 PKCS12 truststore 校验企业私有 CA，Redis Exporter 使用同一 Secret 中的 PEM CA；公有 CA 场景可以关闭自定义 truststore 并使用 JVM 默认信任库。这样既展示 K8s 存储与中间件运维能力，又不会错误声称“一个 StatefulSet Pod 就是生产高可用”。

## 排障顺序

```text
资源状态 → Events → Pod 日志 → Service/EndpointSlice → 探针
→ ConfigMap/Secret 引用 → NetworkPolicy → 恢复与复盘
```

```powershell
kubectl -n opspilot get pods,svc,ingress,hpa,pdb
kubectl -n opspilot get events --sort-by=.lastTimestamp
kubectl -n opspilot describe pod <pod-name>
kubectl -n opspilot logs deployment/opspilot-api --tail=100
kubectl -n opspilot get endpointslice
helm history opspilot -n opspilot
```
