# OpsPilot：Java、Docker 与 Kubernetes 系统学习

本目录记录 OpsPilot 第二阶段学习过程。目标不是收集命令，而是形成以下完整能力链：

```text
理解 Java 业务
→ 构建可运行制品
→ 制作和运行容器
→ 部署到 Kubernetes
→ 发布与回滚
→ 监控与告警
→ 故障定位与恢复
→ 留下可复盘证据
```

## 学习规则

每个主题都采用六步法：

1. **原理**：它解决什么问题。
2. **观察**：查看当前系统的真实状态。
3. **操作**：亲手改变一个可控变量。
4. **故障**：制造或分析一个失败现象。
5. **复述**：不用原文，用自己的话解释。
6. **验收**：根据输出而不是感觉判断是否掌握。

## 14 天路线

| 天数 | 主题 | 状态 |
|---|---|---|
| Day 1 | Docker 对象模型、OpsPilot 三层链路、Java 账户请求链 | 进行中 |
| Day 2 | Kubernetes 控制平面、Pod、Deployment、ReplicaSet、Service | 部分实操，待系统验收 |
| Day 3 | 扩缩容、滚动更新、回滚与常见工作负载故障 | 已做基础操作，待结合 OpsPilot |
| Day 4 | Pod 网络、DNS、Service、EndpointSlice、Ingress | 未开始 |
| Day 5 | ConfigMap、Secret、PV、PVC、StatefulSet | 未开始 |
| Day 6 | 探针、资源限制、HPA、PDB、RBAC、安全上下文 | 未开始 |
| Day 7 | 可观测性、综合排障与第一阶段验收 | 未开始 |
| Day 8 | OpsPilot Kubernetes 架构与代码基线 | 未开始 |
| Day 9 | 应用 Deployment 与 MySQL StatefulSet | 未开始 |
| Day 10 | Ingress、可靠性与优雅停机 | 未开始 |
| Day 11 | Helm 发布、失败门禁与回滚 | 未开始 |
| Day 12 | Prometheus、Grafana 与 Alertmanager | 未开始 |
| Day 13 | 故障演练、备份恢复和 Runbook | 未开始 |
| Day 14 | 一键演示、项目验收与面试讲解 | 未开始 |

## 当前教材

- [Day 1：Docker 与 Java 基础](day-01-docker-java-foundations.md)
- [Day 1：理解检查与实验记录](day-01-checkpoint.md)
- [三节点 Kubernetes × OpsPilot 系统学习路线](three-node-opspilot-learning-path.md)
- [三节点虚拟机实际部署操作手册](../../../deploy/k8s/vm-lab/README.md)

## 重要边界

- Minikube 是本地单节点学习环境；三台虚拟机是单 control plane + 双 worker 实验集群。后者能练跨节点调度，但仍不代表生产高可用集群。
- MySQL 进入 Kubernetes 后，单副本 StatefulSet 只用于学习持久化，不宣称生产高可用。
- 已经执行过的 Flyway `V1` 不允许修改；结构变化必须新增 `V2+`。
- 不把真实数据库密码、Grafana 密码、令牌或私钥提交到 Git。
- 在 Kubernetes 主线完成前，不加入 Istio、Operator、多集群、Jenkins 等扩展技术。
