# NorthLedger 个人学习与项目讲解路线

你的当前基础是 Linux 常用命令和 Docker 基本操作，正在学习计算机网络。因此不要从 Kubernetes YAML 开始背，也不要试图一次记住全部代码。按“原理 → 观察 → 操作 → 故障 → 复述 → 验收”逐层接管项目。

## 第 1 层：先讲通一笔转账

原理：转账不是“两个余额分别更新”，而是一个不可分割的数据库事务。源账户和目标账户按固定顺序加悲观锁，避免并发反向转账形成死锁；余额不足时整个事务回滚；客户端请求号加唯一约束，保证超时重试不会重复扣款。

观察：在前端创建两个账户并发起转账，然后查看订单、两个账户余额和借贷流水。

操作：运行 `TransferFlowIntegrationTest`，对照 `TransferApplicationService` 逐步阅读。

故障：尝试余额不足、相同账户互转、重复请求号和不存在账户，解释为什么失败以及数据是否变化。

复述：用 2 分钟说明“幂等键、事务、锁顺序、双录流水”分别解决什么问题。

验收：不看代码画出转账时会读写的表，并说出任一步失败后的结果。

## 第 2 层：接管登录和权限

原理：认证确认身份，授权限制职责，CSRF 保护带 Cookie 的修改请求，会话保存到带 TTL 的 Redis 以支持多个 API 副本；MySQL 继续保存用户和角色事实。

观察：分别以管理员、操作员、审计员登录，比较菜单与接口结果；用 `redis-cli --scan --pattern 'northledger:session:*'` 观察会话键，用 MySQL 查询 `audit_event`。

操作：阅读 [认证与权限 Runbook](../runbooks/authentication-and-access.md)，运行三组安全集成测试。

故障：制造错误密码、缺失 CSRF、普通用户访问管理员接口、删除浏览器 SESSION。

复述：明确说明 401、403、Cookie、CSRF token 和 RBAC 的关系。

验收：只借助浏览器网络面板、Redis 和 MySQL，定位一次“登录后立即掉线”。

## 第 3 层：掌握 Linux 与网络请求路径

原理：请求经过浏览器、本机端口映射、Nginx、容器网络、Spring Boot、连接池和 MySQL。每层都有地址、端口、进程、日志和健康状态。

观察：使用 `ipconfig`/`ip`、`ss`、`curl`、`docker compose ps`、容器日志和 Nginx 配置确认每一跳。

操作：从 `http://localhost:18000/health` 开始，依次验证 Nginx、后端 readiness 和数据库连接。

故障：停止 API、停止 MySQL、占用端口、写错数据库主机名，记录浏览器现象、HTTP 状态和日志证据。

复述：解释宿主机 `18000/18080/3306`、容器端口、服务名 DNS 和反向代理分别做什么。

验收：面对“网页打不开”，按 DNS → TCP → HTTP → 应用 → 数据库的层次排查，而不是随机重启。

## 第 4 层：掌握 Docker Compose 交付

原理：镜像是只读交付物，容器是运行实例，volume 保存状态，network 提供服务发现，healthcheck 决定是否就绪。

观察：查看镜像层、非 root UID、只读文件系统、volume 和网络别名。

操作：完整启动基础栈和观测栈，执行升级重建，确认 MySQL 数据没有因 API 容器重建丢失。

故障：构建 JAR 缺失、环境变量缺失、容器 unhealthy、旧 DNS、磁盘空间不足。

复述：说明 Dockerfile 多阶段构建、Compose 编排和 Nginx 统一入口的职责边界。

验收：在不删除数据库 volume 的前提下重建 API/Web，并验证旧账户、登录和转账仍可用。

## 第 5 层：掌握可观测性与首响排障

原理：指标回答趋势，日志解释过程，审计保存责任事实，健康检查回答是否接流量，告警负责主动通知。

观察：在 Grafana 看 HTTP 延迟与错误，在 Prometheus 查认证失败趋势，在 Loki 用 traceId 查日志，在审计页查操作者。

操作：执行一次正常登录、失败登录和转账，用四类证据串起时间线。

故障：Prometheus target down、Grafana 无数据、Loki 未采集、告警一直 pending。

复述：说明 Prometheus 不是接口文档；Alertmanager 不是指标查询器；Grafana 不是数据源。

验收：给出一次故障的现象、影响、时间线、根因、恢复动作和防复发措施。

## 第 6 层：最后学习 Kubernetes/Helm

原理：Deployment 管无状态副本，Service 提供稳定发现，Ingress 提供外部入口，探针决定重启与摘流，HPA 扩缩容，PDB 约束主动驱逐，NetworkPolicy 限制网络访问，Secret 注入敏感配置。

观察：使用 `kubectl get/describe/logs/events` 理解期望状态与实际状态，不先背 YAML。

操作：通过 Helm 部署到 Minikube，执行滚动升级、失败发布和回滚；再到三节点学习集群观察调度与节点故障。

故障：`ImagePullBackOff`、探针失败、PVC Pending、Secret 缺失、Service selector 错误、Ingress 不通。

复述：说明 Docker Compose 与 Kubernetes 解决问题的规模差异，以及为什么内置单实例 MySQL 不能宣称生产高可用。

验收：从 Pod 事件定位一次镜像拉取失败；从 Service/Endpoints 定位一次入口不通；完成一次 Helm 回滚。

## 面试讲解顺序

1. 业务问题：账户转账为什么需要幂等、事务和双录。
2. 架构选择：为什么是模块化单体和 MySQL，而不是微服务与组件堆砌。
3. 安全治理：会话、RBAC、CSRF 和审计如何形成闭环。
4. 运维交付：Linux、Compose、Helm 三条路径如何共享同一镜像和配置原则。
5. 可观测与恢复：如何从告警进入指标、日志、审计，再完成恢复与复盘。
6. 真实边界：哪些已经亲自完成，哪些只在 Minikube/实验集群验证，哪些是生产演进方向。

项目可以先完成，能力必须逐层接管。每层只有在你能独立观察、制造故障、排除故障并复述后，才从“项目里有”变成“你会”。
