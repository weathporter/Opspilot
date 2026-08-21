# OpsPilot Kubernetes 交付

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
  → MySQL StatefulSet + PVC（本地学习）
     或外部托管 MySQL（类生产）
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
| 网络边界 | 默认拒绝入站，Web→API→MySQL 逐段放行 | 把调用链落实为最小网络权限 |
| 权限边界 | 工作负载不挂 Token；operator Role 不读 Secret | 避免业务 Pod 获得无用集群权限 |
| 启动时序 | API initContainer 等待数据库 TCP 就绪 | 避免 Flyway 在 MySQL 初始化期失败并触发无意义重启 |
| 数据持久化 | MySQL StatefulSet + PVC | 用于学习 Pod 重建后数据保留 |
| 配置密钥 | ConfigMap 与 Secret 分离 | 公开配置与敏感值使用不同生命周期 |
| 发布验收 | Helm test 访问 Web Service `/health` | 在集群内验证 Nginx→API→MySQL readiness 链路 |

## 本机部署

前提：Docker Desktop、Minikube、kubectl、Helm。当前项目使用 Docker driver。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1
```

脚本会检查每个外部命令的退出状态，启动或复用 Minikube，构建 API/Web 运行时镜像，显式加载 API、Web、MySQL 三个镜像，以 Helm 4 的 `--rollback-on-failure` 执行失败自动回滚，最后验证 rollout、Release Test 和资源状态。

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

它会真实创建两个账户、完成转账、用相同幂等键重放，并检查两条流水和 `balanced=true`。

## 发布与回滚

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1 -Version 0.1.1
powershell -ExecutionPolicy Bypass -File scripts/k8s/rollback.ps1
```

Helm 保存应用发布历史，Deployment 保留 5 个 ReplicaSet revision；前者负责应用级回滚，后者便于单工作负载排障。

## 数据库边界

默认单副本 MySQL StatefulSet 是学习环境，不是生产高可用方案。它用于证明稳定身份、PVC、Secret、探针、资源限制和 NetworkPolicy。

类生产部署使用 `values-production.example.yaml`，关闭内置 MySQL，连接企业托管数据库，并要求外部密钥系统预建 Secret。这样既展示 K8s 存储能力，又不会错误声称“一个 MySQL Pod 就是生产高可用”。

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
