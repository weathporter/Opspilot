# Kubernetes 企业级边界

项目实现的是“production-shaped”：资源对象、探针、安全上下文、滚动发布、权限和网络边界采用生产思路，但运行环境仍是个人电脑上的单节点 Minikube。

| 项目实现 | 企业生产环境还需要 |
| --- | --- |
| Minikube 单节点 | 多控制平面、多工作节点、跨可用区 |
| HPA CPU 指标 | 业务指标扩缩容、容量基线、压测数据 |
| PDB minAvailable=1 | 多节点分布、集群自动扩容和维护策略配合 |
| NetworkPolicy | 支持策略的 CNI、出口策略、DNS 与监控例外 |
| 本地 Secret | Vault/KMS/External Secrets、轮换与审计 |
| 单副本 MySQL + PVC | 托管高可用 MySQL、备份演练、RPO/RTO |
| 本地 Ingress HTTP | TLS、WAF、证书自动化、真实域名和限流 |
| Helm 手动升级 | CI 门禁、镜像扫描、制品签名、审批与 GitOps |

## 暂不加入的组件

- Service Mesh：当前没有跨服务 mTLS、复杂流量治理或大量微服务调用链。
- Operator：项目没有需要自定义控制循环维护的领域资源。
- Argo CD/Jenkins：先把镜像、Helm、滚动发布和回滚链讲清楚；流水线是下一层自动化。
- Milvus：当前是精确账户/订单查询和时序监控，不存在语义向量检索需求。

这些不是“永远不用”，而是在当前业务规模下没有足够收益。面试中能说明采用条件，比单纯堆组件更接近工程判断。
