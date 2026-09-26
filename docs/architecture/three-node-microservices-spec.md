# NorthLedger 三节点微服务改造规格

> 状态：服务代码、独立测试、镜像与 Helm/CI 声明已形成；真实三节点运行、发布审核与故障演练仍需操作者在虚拟机上验收。不能把静态配置写成已部署能力。

> 部署档位更新：当前 15 GiB 级宿主机先采用[两节点低资源档位](../runbooks/two-node-microservices-release.md)打通完整链路；本文的三节点设计保留为扩展目标，不代表两节点已经具备跨工作节点故障接管。

## 目标与边界

- 在现有 `D:\Develop\OpsPilot` 项目中实现三个可独立构建、发布和扩缩容的后端服务：`access-service`、`ledger-service`、`operations-service`。
- 保留现有 React 页面和 `/api/v1` 对外接口；浏览器只访问 Web/Nginx 与 Access 服务。
- 保留 Redis 服务端会话与 CSRF。Access 完成用户认证和 RBAC，使用受保护的内部凭据调用 Ledger 与 Operations。业务服务拒绝无凭据的请求，不信任浏览器传来的内部身份头。
- 一台 MySQL 实例内按服务划分逻辑数据库与最小权限账号；每个服务只修改自己拥有的表。Redis 仅承载会话和可重建缓存，不保存资金事实。
- 在三节点 Kubernetes 实验集群部署：一个控制平面、两个工作节点。单控制平面和单副本数据库不是高可用生产集群，不作高可用宣称。
- GitHub Actions 完成测试、镜像构建与发布；受保护的部署作业在能够访问集群的执行器上完成 Helm 升级、冒烟测试与失败回滚。集群实际操作由用户执行，仓库提供配置和 Runbook。

## 服务与数据所有权

| 服务 | 职责 | 独占数据 | 允许依赖 |
| --- | --- | --- | --- |
| Access | 登录、会话、用户与角色、安全审计、对外 API 聚合 | `app_user`、`app_user_role`、`audit_event` | Redis 会话；Ledger/Operations 内部 HTTP |
| Ledger | 账户、转账、幂等、订单与双边流水、只读统计接口 | `account`、`transfer_order`、`ledger_entry` | MySQL |
| Operations | 看板聚合、对账执行与历史 | 对账批次与差异结果表 | Ledger 内部只读 HTTP；Redis 可重建缓存 |

转账及借贷流水保持在 Ledger 的单个本地事务中；跨服务调用不参与资金写事务。Operations 读取 Ledger 统计接口，允许短暂最终一致，不能跨库直接查询 Ledger 表。原 Flyway V1/V2 不改写；迁移时保留历史数据，并为新逻辑库建立各自独立的迁移历史。

## 请求与信任边界

`Browser -> Web/Nginx -> Access -> Ledger/Operations -> 各自数据库`。

Access 继续验证 SESSION、CSRF 与角色；内部调用使用固定目标 Service DNS、超时、请求追踪号以及从 Secret 注入的服务凭据。Ledger/Operations 的内部端点必须先验证服务凭据；NetworkPolicy 只放行 Access 与必要的监控源。客户端不能决定内部目标 URL，也不能自行提供可信的操作者身份。

## 交付与验收

1. 三个后端服务分别构建镜像，分别拥有探针、资源请求/限制、Service、Deployment、指标与日志标识。
2. MySQL/Redis 使用 StatefulSet 与 PVC；记录所在节点、备份、离集群恢复和节点故障后的限制。服务滚动更新与数据库迁移采用向前兼容顺序。
3. CI 在每次 PR 执行测试与 Helm 校验；主分支通过后发布带提交 SHA 的镜像。CD 仅使用受保护的部署环境，部署确定的 SHA，不使用浮动 `latest`。
4. 冒烟测试覆盖登录、角色拒绝、开户、幂等转账、双边流水、看板和对账；演练 Pod 重建、发布失败回滚、Redis 故障与数据库备份恢复。
5. README 和 Runbook 清楚区分“代码/静态配置已验证”和“用户在三节点集群亲自验证”；没有集群实测不得声称三节点闭环已通过。

## 不增加的组件

本次不引入 Kafka、Nacos、Service Mesh、Jenkins、GitLab 或 Milvus。服务发现使用 Kubernetes Service DNS；同步协作使用 Spring Framework 自带的 HTTP 客户端；镜像仓库使用 GHCR。
