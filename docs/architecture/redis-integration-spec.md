# NorthLedger Redis 缓存与共享会话升级规格

## 1. 目标与范围

NorthLedger 0.3.0 在不拆分模块化单体、不增加消息队列等无关组件的前提下，引入 Redis 解决两个已经存在的工程问题：

1. 运维总览每次刷新都会重复执行账户、转账、金额合计、24 小时趋势和最近交易查询；该只读快照允许几十秒级最终一致，适合缓存。
2. API 采用多副本部署时需要共享登录态；会话数据不应继续占用 MySQL 业务连接和关系表清理任务，改由具备 TTL 语义的 Redis 保存。

本次只新增 Spring Data Redis、Spring Cache、Spring Session Data Redis 和 Redis Exporter。Kafka、Milvus、Redis Cluster、Sentinel、服务网格与分布式锁不进入本次范围。

## 2. 架构边界

MySQL 仍是账户、余额、转账订单、双边流水、用户与审计记录的唯一事实来源。Redis 只保存可重建的总览快照和有过期时间的会话；任何时候都不能把 Redis 中的数据当作资金事实。

本地 Compose 与 Minikube 使用一个受密码保护的单节点 Redis，以降低 4 GiB 学习环境的资源成本。缓存键和会话键分别使用 `northledger:cache:` 与 `northledger:session` 命名空间。共享实例使用 `noeviction`，避免内存压力下静默淘汰仍有效的会话；所有业务缓存和会话必须带 TTL。

生产示例默认关闭 Chart 内置 Redis，连接平台提供的高可用 Redis。单节点、单密码、无 TLS 的本地形态只用于开发与运维实践，不宣称生产高可用。

## 3. 业务缓存契约

唯一缓存对象是 `OperationsDashboardApplicationService.getSummary()` 返回的 `OperationsSummaryResponse`：

- 缓存名：`operations-summary`。
- 逻辑键：固定值 `current`，因为当前总览没有租户、筛选条件或分页维度。
- 实际前缀：`northledger:cache:operations-summary::`，保留 Spring Cache 的缓存名隔离。
- TTL：基础 30 秒，加 0～10 秒随机抖动，降低多个实例同时回源形成缓存击穿的概率。
- 空值：禁止缓存，避免暂时性异常被固化。
- 序列化：JSON；不使用 Java 原生对象序列化，降低不可信字节流反序列化风险并提高排障可读性。

第一次读取未命中时执行原有 MySQL 聚合并写入 Redis；TTL 内再次读取直接返回快照。缓存命中不得改变 API JSON 契约。

## 4. 一致性与失效

账户创建和新转账成功都会改变总览。写服务在数据库事务内发布无敏感信息的 `BusinessDataChangedEvent`，监听器只在 `AFTER_COMMIT` 阶段清空 `operations-summary`。

这条顺序保证：

1. 数据库提交失败时不清缓存，旧快照仍对应最后一次成功提交。
2. 数据库提交成功后才清缓存，下一次读取从 MySQL 重建。
3. 幂等转账重放只返回既有结果，不重复发布变更事件。

失效动作本身失败时记录降级日志和指标，但不回滚已经成功的资金事务；30～40 秒 TTL 是最终兜底。该取舍必须在面试中明确说明：资金写入正确性优先于总览的秒级新鲜度。

## 5. 故障降级与真实边界

缓存访问使用自定义 `CacheErrorHandler`：

- `get` 失败被视为未命中，继续执行 MySQL 查询。
- `put`、`evict`、`clear` 失败被记录但不向业务调用方抛出。
- 日志只包含稳定事件名、操作、缓存名、异常类型和现有 `traceId`；不得记录密码、Cookie、缓存值、账户姓名或完整请求体。
- 指标 `northledger_cache_errors_total{operation,cache}` 只使用固定操作和已声明缓存名，避免高基数。

Redis 同时承载会话，因此 Redis 故障时：已经进入服务层的总览查询可以降级到 MySQL，但需要认证的新 HTTP 请求可能因会话无法读取而失败。这不是缓存降级代码能掩盖的边界，告警和 Runbook 必须如实区分“缓存退化”和“登录态依赖不可用”。

## 6. 会话契约

Spring Session 使用 Redis 默认仓储替代 JDBC：

- `spring.session.store-type=redis`。
- 命名空间：`northledger:session`。
- 超时：默认 30 分钟，可由 `SESSION_TIMEOUT` 覆盖。
- 写入时机：`on_save`，一次请求结束时保存，减少无意义往返。
- Cookie 保持 HttpOnly、SameSite=Lax；生产 HTTPS 继续要求 Secure。

已经由 Flyway V2 创建的 `SPRING_SESSION*` 表保留为历史兼容结构，不修改已执行迁移，也不新增无意义的删除迁移；0.3.0 运行时不再使用这些表。

## 7. 安全威胁模型

| 威胁 | 风险 | 控制 |
| --- | --- | --- |
| 未授权访问 Redis | 读取或篡改会话、缓存 | 密码从环境/Secret 注入；端口仅回环或 ClusterIP；NetworkPolicy 只放行 API 与 exporter |
| 密码泄露 | 日志、进程参数、Git 历史暴露 | 不提交真实密码；不写日志；Compose/K8s 从环境或 Secret 注入；提交前扫描 |
| 会话被内存淘汰 | 用户无故退出 | 共享实例使用 `noeviction`，缓存和会话均设置 TTL，并告警内存逼近上限 |
| 恶意缓存字节流 | 反序列化执行或类型混淆 | 业务缓存使用 JSON，不启用 Java 原生反序列化；Redis 不对公网开放 |
| Redis 故障放大数据库压力 | 大量缓存未命中同时回源 | 连接/命令超时、短 TTL 抖动、缓存错误指标、Runbook 限流与恢复步骤 |
| 明文网络窃听 | 跨不可信网络泄露 AUTH/会话 | 本地网络仅受信任主机/集群；生产外部 Redis 必须使用受控私网并按平台能力启用 TLS |

## 8. 可观测性问题与信号

值班人员必须能回答：

1. 总览请求是在命中缓存还是因 Redis 错误回源 MySQL？
2. Redis 是否可达，内存是否接近上限，是否出现拒绝写入或意外淘汰？
3. Redis 故障是否已经影响登录会话和用户请求？
4. 账户/转账成功后缓存是否被及时失效？

对应信号：

- Spring Cache/Micrometer 缓存统计与 `northledger_cache_errors_total`。
- Redis Exporter 的 `redis_up`、内存、连接、命令、过期和淘汰指标。
- `cache_operation_degraded` 与 `operations_summary_cache_evicted` 结构化日志。
- `RedisUnavailable`、`RedisMemoryPressure`、`RedisUnexpectedEviction` 告警，每条告警链接 Redis Runbook。
- Grafana Redis 看板展示可用性、内存、连接、命中/未命中、淘汰、缓存降级次数。

## 9. 部署矩阵

| 环境 | Redis 来源 | 连接方式 | 持久化与边界 |
| --- | --- | --- | --- |
| IDEA + Testcontainers | 测试自动启动 | 动态主机和端口 | 测试密码、无生产数据 |
| Docker Compose | `redis:7.4.10-alpine` | `redis:6379` | AOF everysec、命名卷、128 MiB、noeviction |
| Linux systemd | 同机受控 Redis 或平台 Redis | 环境变量 | 应用不负责安装生产集群；提供配置、健康与排障说明 |
| Minikube/Helm | 可选内置 StatefulSet | ClusterIP | 单副本 PVC，仅学习实践 |
| 生产 Helm 示例 | 外部高可用 Redis | 平台 DNS/Secret | TLS、备份、故障转移由平台能力和生产方案治理 |

统一环境变量为 `REDIS_HOST`、`REDIS_PORT`、`REDIS_USERNAME`、`REDIS_PASSWORD`、`REDIS_CONNECT_TIMEOUT`、`REDIS_TIMEOUT`。真实密码只允许出现在本机 `.env`、Linux 权限受控环境文件或 Kubernetes Secret 中。

## 10. 验收标准

1. 先看到缺少 Redis 行为的测试失败，再完成实现并通过。
2. 两次连续读取总览得到同一 `generatedAt`，Redis 中存在带 30～40 秒 TTL 的缓存键。
3. 成功开户或新转账提交后缓存被清除，下一次总览反映新数据；失败事务不产生成功失效事件。
4. Redis 缓存读写故障时，直接调用总览服务仍能回源 MySQL，并增加有界错误指标。
5. MockMvc 登录后 Redis 出现 `northledger:session` 会话键，旧 JDBC 会话表不再新增记录。
6. Compose Redis 通过认证健康检查，端口只绑定 `127.0.0.1`，应用和 exporter 都能连接。
7. Prometheus 能抓取 Redis Exporter；Grafana 看板与三条 Redis 告警规则可解析。
8. Helm 本地模式能渲染 Redis StatefulSet、Service、Secret、PVC、exporter 和 NetworkPolicy；生产示例能切换外部 Redis。
9. Linux 环境模板、健康检查、巡检、诊断收集和故障演练覆盖 Redis。
10. Maven 全量测试、前端构建、Compose 配置、Prometheus 规则、Helm lint/render、脚本测试和敏感信息扫描均有新鲜成功证据。

## 11. 官方实现依据

- Spring Boot 3.5 Cache 与 Redis 配置：https://docs.spring.io/spring-boot/3.5/reference/io/caching.html
- Spring Data Redis Cache TTL：https://docs.spring.io/spring-data/redis/reference/3.5/redis/redis-cache.html
- Spring Data Redis 序列化安全说明：https://docs.spring.io/spring-data/redis/reference/3.5/redis/template.html
- Spring Session 3.5 Redis：https://docs.spring.io/spring-session/reference/3.5/guides/boot-redis.html
- Spring Session Redis 命名空间与仓储：https://docs.spring.io/spring-session/reference/3.5/configuration/redis.html
- Spring 事务绑定事件：https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
- Redis 安全：https://redis.io/docs/latest/operate/oss_and_stack/management/security/
- Redis 持久化：https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- Redis 内存淘汰：https://redis.io/docs/latest/develop/reference/eviction/
- Redis Exporter：https://github.com/oliver006/redis_exporter/blob/master/README.md
