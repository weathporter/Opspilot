# NorthLedger Redis 缓存、会话与运维学习手册

这份手册不是 Redis 命令清单，而是帮助你把 NorthLedger 0.3.0 的 Redis 设计真正讲明白、跑起来、观察到、制造故障并恢复。建议按“原理 → 对象 → 请求链路 → 操作 → 故障 → 复述 → 验收”学习。

## 1. 先记住项目的核心边界

NorthLedger 同时使用 MySQL 和 Redis，但两者地位不同：

| 数据 | 保存位置 | 是否是业务事实 | 丢失后的含义 |
| --- | --- | --- | --- |
| 账户、余额、交易订单、借贷流水 | MySQL | 是 | 资金业务数据丢失，属于严重事故 |
| 用户、角色、密码哈希、审计事件 | MySQL | 是 | 无法认证或追责，属于严重事故 |
| 浏览器登录会话 | Redis | 否，但影响在线服务 | 用户需要重新登录；Redis 不可用时登录态无法读取 |
| 运行总览缓存 | Redis | 否 | 可以重新查询 MySQL 生成，延迟和数据库压力上升 |

一句话复述：**MySQL 是不可丢的事实库，Redis 是有明确生命周期的共享临时状态。**

不要说“把数据放进 Redis 是为了更快”就结束。你还要回答：缓存了什么、为什么能缓存、保存多久、何时失效、故障后怎么退、如何证明它在工作。

## 2. 为什么这个项目需要 Redis

### 2.1 共享登录会话

Kubernetes 中 API 通常有多个 Pod。第一次登录落到 Pod A，下一次请求可能被 Service 转到 Pod B。如果会话只在 Pod A 的 JVM 内存中，Pod B 不认识这个用户；如果 Pod A 重启，所有本机会话也会消失。

Spring Session 把标准 `HttpSession` 保存到 Redis 后，Pod A 和 Pod B 都使用同一共享存储读取登录态，不需要依赖负载均衡器的粘性会话。Redis 原生 TTL 还可以自动清理过期会话。

### 2.2 运行总览重复聚合读

运行总览会统计账户数量、转账数量、成功率、金额和最近交易。一次刷新需要跨多个 Repository 查询 MySQL，而首页会被重复打开。它具有三个适合缓存的特征：

1. 只读，能够从 MySQL 完整重建。
2. 不要求每毫秒都变化，30～40 秒最终一致可接受。
3. 多个用户请求的是同一份当前总览，缓存复用率高。

账户详情、转账提交和余额没有被缓存。它们对实时性与一致性要求更高，引入缓存反而容易让学习边界失控。

## 3. 代码对象与职责

| 对象 | 位置 | 作用 |
| --- | --- | --- |
| `RedisCacheConfiguration` | `src/main/java/com/opspilot/config` | 定义缓存名、JSON 序列化、键前缀、TTL 抖动、事务感知和错误处理器 |
| `DashboardCacheNames` | `src/main/java/com/opspilot/dashboard` | 集中定义允许使用的缓存名，避免各处拼错字符串 |
| `OperationsDashboardApplicationService` | `src/main/java/com/opspilot/dashboard` | 使用 `@Cacheable` 缓存当前运行总览 |
| `BusinessDataChangedEvent` | `src/main/java/com/opspilot/dashboard` | 只表达“会影响总览的数据已改变”，不携带账户和交易敏感内容 |
| `OperationsSummaryCacheInvalidator` | `src/main/java/com/opspilot/dashboard` | 在 MySQL 事务成功提交后清理总览缓存 |
| `CacheFailureTelemetry` | `src/main/java/com/opspilot/observability` | 缓存失败时回源，并记录低基数指标和结构化日志 |
| `application.yml` | `src/main/resources` | 配置 Redis 连接、Spring Session 命名空间、超时和健康检查 |

这里仍然是模块化单体。引入 Redis 没有把系统拆成微服务，也没有改变转账事务仍由一个 Spring Boot 进程和一个 MySQL 数据库完成的事实。

## 4. 一次总览请求如何经过缓存

```text
浏览器 GET /api/v1/operations/summary
              │
              ▼
Spring Security 从 Redis 读取 SESSION
              │
              ▼
OperationsDashboardApplicationService.getSummary()
              │
              ├─ Redis 中有 current ──▶ 反序列化 JSON ──▶ 直接返回
              │
              └─ Redis 中没有 current
                         │
                         ├─ 查询 MySQL 账户/订单统计
                         ├─ 组装 OperationsSummaryResponse
                         ├─ 写入 Redis，TTL 30～40 秒
                         └─ 返回响应
```

`@Cacheable(cacheNames = "operations-summary", key = "'current'")` 由 Spring AOP 代理执行。调用者不是直接先进入方法体，而是先经过缓存拦截器：命中就跳过方法；未命中才执行方法并缓存结果。

当前 Redis 键的稳定形式是：

```text
northledger:cache:operations-summary::current
```

固定键 `current` 是因为接口暂时没有租户、日期和筛选条件。如果以后增加 `tenantId`，键必须包含租户维度，否则会发生跨租户数据串读；这也是为什么缓存键设计属于数据隔离设计，而不只是字符串拼接。

## 5. TTL、抖动和主动失效

### 5.1 为什么必须有 TTL

TTL 是最后一道一致性和容量保护：即使一次主动失效失败，旧总览最多保留约 40 秒；长期无人访问的缓存也会自动消失。没有 TTL 的可重建缓存会永久占用内存，并可能无限期返回旧数据。

### 5.2 为什么是 30～40 秒

30 秒是本项目对首页读模型的演示性新鲜度边界，不适用于余额接口。额外 0～10 秒随机抖动让多个实例、多个键不会在同一时刻集中失效并同时打到 MySQL，这类瞬时回源峰值常被称为缓存雪崩的一种表现。

代码中的 `JitteredTtlFunction` 在每次写入时计算：

```text
实际 TTL = 30 秒 + [0, 10 秒]随机值
```

随机数不是密码学用途，只用于摊平时间，所以使用 `ThreadLocalRandom` 合理。

### 5.3 为什么还要主动失效

只等 TTL 意味着开户或转账完成后，首页可能持续显示旧统计 30 多秒。账户与转账服务在业务事务内发布 `BusinessDataChangedEvent`，监听器使用 `AFTER_COMMIT`：

```text
业务写入开始
  ├─ MySQL 提交失败 ──▶ 不触发缓存清理，原缓存仍对应原数据库状态
  └─ MySQL 提交成功 ──▶ 清理 operations-summary ──▶ 下次请求重新聚合
```

关键不是“用了事件”，而是**清理发生在事务成功之后**。如果在提交前清理，其他请求可能先重建出旧数据；如果事务最终回滚，也会产生一次无意义的缓存抖动。

## 6. Redis 故障时为何不是所有功能都能降级

运行总览缓存和登录会话共用 Redis 实例，但故障语义不同：

- 缓存读取失败：`CacheFailureTelemetry` 不重新抛出异常，Spring 把它当作未命中，继续查询 MySQL。
- 缓存写入或清理失败：本次业务仍可完成，记录指标和日志；旧值由短 TTL 兜底。
- 会话读取失败：请求在进入 Controller 前就无法确认用户身份，不能安全地“假设用户已登录”。因此 Redis 完全不可用会影响登录和已登录请求。

这就是 readiness 将 `redis` 与 `db` 都列为关键依赖的原因。Redis 失败后实例应停止接收新流量；liveness 只看进程自身，避免外部依赖抖动导致 Pod 被反复重启。

面试时不要说“Redis 挂了系统完全无感”。准确说法是：**业务缓存具备回源降级，资金事实不丢；共享会话仍是在线流量的关键依赖，因此用 readiness 摘流、告警和高可用外部 Redis 处理。**

## 7. 序列化和安全边界

缓存值使用 `Jackson2JsonRedisSerializer<OperationsSummaryResponse>`，而不是 Java 原生对象序列化。好处是数据格式明确、可观察，并避免打开通用 Java 反序列化入口。缓存不保存 null，防止“暂时查不到”被长期误认为真实结果。

Redis 密码没有源码默认值。Compose 的 Redis 启动脚本完成以下动作：

1. 从部署环境接收 `REDIS_PASSWORD`，缺失就拒绝启动。
2. 计算 SHA-256 ACL 密码哈希并写入 `/run/redis/users.acl`。
3. ACL 文件位于 tmpfs，不进入镜像层和持久化卷。
4. 以固定非 root UID/GID 运行 Redis。
5. 清除 shell 变量，`redis-server` 进程参数只引用 ACL 文件，不出现明文密码。
6. 使用 `protected-mode yes`、`noeviction`、AOF everysec，并只映射到宿主机 `127.0.0.1:16379`。

这仍不是完整生产密钥系统：Docker 管理员可以查看容器配置中的环境变量。生产环境应通过平台 Secret 注入、使用受控外部高可用 Redis，并按组织要求配置 TLS、凭据轮换和审计。

## 8. 本地启动与基础观察

先复制环境模板并只在本机 `.env` 中设置强密码：

```powershell
Copy-Item .env.example .env
# 编辑 DB_PASSWORD、MYSQL_ROOT_PASSWORD、REDIS_PASSWORD、
# BOOTSTRAP_ADMIN_PASSWORD 和 GRAFANA_ADMIN_PASSWORD。
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
```

查看服务状态：

```powershell
docker compose -f compose.yml -f compose.local.yml -f compose.observability.yml -f compose.desktop-observability.yml ps
```

预期 `mysql`、`redis`、`app`、`nginx` 为 healthy；Prometheus、Grafana、Alertmanager、Loki、Alloy、Redis Exporter 等为 running。

在容器内部执行 Redis 命令可以避免把密码展开到宿主机命令行：

```powershell
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli PING'
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli INFO memory'
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli INFO stats'
```

预期：

- `PING` 返回 `PONG`。
- `INFO memory` 中 `maxmemory` 约为 128 MiB，`maxmemory_policy:noeviction`。
- `INFO stats` 中能够看到 `keyspace_hits`、`keyspace_misses`、`evicted_keys` 等计数。

## 9. 亲自证明缓存真的工作

先登录 NorthLedger，在浏览器网络面板连续请求两次运行总览，然后执行：

```powershell
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --scan --pattern "northledger:cache:*"'
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli TTL "northledger:cache:operations-summary::current"'
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli GET "northledger:cache:operations-summary::current"'
```

你应该观察到：

1. 第一次请求后出现缓存键。
2. TTL 通常处于 1～40 秒，刚写入时应处于 30～40 秒。
3. GET 返回 JSON 总览，不是不可读的 Java 序列化字节。
4. TTL 到期或成功开户/转账后，键被删除；下一次请求重新出现。

不要用 `KEYS *` 做日常生产排查。它会遍历整个键空间并可能阻塞大实例；学习和排障使用渐进式 `SCAN`。

## 10. 亲自证明会话存于 Redis

登录前后分别执行：

```powershell
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --scan --pattern "northledger:session:*"'
```

登录后应看到 Spring Session 生成的命名空间键。选择一个键检查：

```powershell
docker compose -f compose.yml -f compose.local.yml exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli TTL "<会话键>"'
```

TTL 应为正数，默认会话超时约 30 分钟。浏览器端只能看到随机 `SESSION` Cookie，真正的认证属性位于服务端 Redis。退出后旧会话应不能继续访问受保护接口。

不要把会话键、Cookie 或 Redis 值粘贴到公开工单和截图中，它们属于认证相关数据。

## 11. Prometheus、Grafana 和 Alertmanager 分别做什么

Redis Exporter 连接 Redis，读取 INFO 等运行信息，并在 `:9121/metrics` 转为 Prometheus 文本指标。它不是 Redis 管理界面，也不保存历史数据。

Prometheus 周期性抓取 Redis Exporter 与 Spring Boot Actuator，保存时间序列并计算规则。常用查询：

```promql
redis_up{job="redis"}
up{job="redis"}
redis_memory_used_bytes / redis_memory_max_bytes
increase(redis_evicted_keys_total[5m])
sum by (operation) (rate(northledger_cache_errors_total[5m]))
```

Grafana 查询 Prometheus 并展示 Redis 状态、内存、客户端、命令速率、命中/未命中、淘汰和应用缓存错误。Grafana 不采集指标，也不是 Redis 本身。

Alertmanager 接收 Prometheus 已经判定为 firing 的告警，负责分组、静默、抑制和通知状态。`http://localhost:19093/#/alerts` 为空通常意味着当前没有 firing 告警，不是页面坏了。

本项目定义：

- `RedisUnavailable`：`redis_up=0` 或 exporter 整次抓取 `up=0` 持续 1 分钟；两种 exporter 失败表现都不会漏报。
- `RedisMemoryPressure`：内存使用比例持续超过 85%。
- `RedisUnexpectedEviction`：5 分钟内出现淘汰键；当前 `noeviction` 策略下不应发生。

## 12. 执行有界故障演练

仅在本地实验环境执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/redis-fault-drill.ps1 -Confirm LAB_ONLY -DurationSeconds 20
```

脚本只解析当前 Compose 项目的 Redis 容器，停止最多 120 秒，并在 `finally` 中恢复。演练期间观察：

1. `docker compose ps` 中 Redis 停止。
2. 应用 readiness 因关键依赖失败而转为不可用。
3. 已进入服务层的总览缓存操作产生 `cache_operation_degraded` 日志与 `northledger_cache_errors_total`。
4. 持续足够时间后，Prometheus 规则从 pending 变为 firing，Alertmanager 接收告警。
5. Redis 恢复后 PING、readiness、登录、总览和告警状态逐步恢复。

如果演练中断，先运行：

```powershell
docker compose -f compose.yml -f compose.local.yml up -d redis
```

然后按 [Redis 降级 Runbook](../runbooks/redis-degradation.md) 检查，不要删除数据卷。

## 13. 常见问题与排查路径

### 13.1 Redis 容器 unhealthy

按顺序检查：

1. `.env` 是否提供 `REDIS_PASSWORD`，不要打印具体值。
2. `docker compose logs redis` 是否有 ACL、权限、AOF 或只读文件系统错误。
3. 健康检查是否返回 PONG。
4. 16379 是否只绑定 127.0.0.1，容器间应使用 `redis:6379`，不是宿主端口。

### 13.2 应用报 Redis 认证失败

应用、Redis 和 Redis Exporter 必须引用同一个部署 Secret。只修改其中一个值会造成客户端认证失败。不要临时关闭 Redis 认证；修正 Secret 后按受控顺序重建客户端。

### 13.3 登录后立刻变成 401

先看浏览器是否发送 SESSION，再扫描 `northledger:session:*`，检查 TTL、Redis 连接、Cookie Domain/Path/SameSite/Secure。HTTP 本地环境使用 `SESSION_COOKIE_SECURE=false`；HTTPS 生产入口应设为 true。

### 13.4 总览一直是旧数据

检查缓存 TTL 是否在递减；完成一次开户后键是否被删除；查看是否出现 `clear`/`evict` 类型的缓存错误指标。MySQL 事务回滚时不清缓存是正确行为。

### 13.5 Redis 内存达到上限

先统计会话与缓存键数量、TTL 和增长速度。`noeviction` 会拒绝新写入而不是静默淘汰会话，这是有意选择；容量不足时要告警和扩容，不能靠随机淘汰隐藏问题。

### 13.6 Prometheus 有 target，但 Grafana 没图

先在 Prometheus 直接查询 `redis_up`。没有数据时检查 scrape target 和 exporter；有数据时再检查 Grafana 数据源、时间范围和变量。按数据流逐层查，不要先重装 Grafana。

## 14. Docker Compose 与 Kubernetes 的对应关系

| Compose | Kubernetes/Helm | 共同目的 |
| --- | --- | --- |
| `redis` service | Redis StatefulSet + PVC | 本地/实验环境保存会话和缓存 |
| 服务名 `redis` | Service `opspilot-redis` | 为客户端提供稳定 DNS 与端口 |
| `.env` | Secret | 注入密码，不写进镜像 |
| healthcheck | startup/readiness probe | 判断 Redis 是否可服务 |
| volume `redis_data` | PVC | 普通容器/Pod 重建后保留 AOF |
| Compose network | Service + NetworkPolicy | 服务发现和受控访问 |
| `redis-exporter` service | Exporter Deployment/Service | 暴露 Prometheus 指标 |

Helm 默认值只面向 Minikube 学习：一个 Redis StatefulSet 不等于生产高可用。`values-production.example.yaml` 关闭内置 Redis，改连企业托管的高可用端点。你可以据此讲出“开发/实验环境自包含，生产环境外部化状态组件”的常见交付策略。

## 15. Linux 原生部署需要理解什么

Linux systemd 路径不会在应用里嵌入 Redis。环境文件配置 `REDIS_HOST`、`REDIS_PORT`、`REDIS_USERNAME`、`REDIS_PASSWORD` 与超时；`inspection.sh` 和 `collect-diagnostics.sh` 负责收集端口、进程、服务、健康和 Redis 连接线索，但不输出密码。

排查时按层次进行：

```text
DNS/主机名解析
  → TCP 端口可达
  → Redis TLS/ACL 认证
  → PING/命令响应
  → Spring Boot Redis 健康
  → Spring Session/Cache 具体行为
```

这条路径与学习计算机网络直接相关：连接拒绝、超时、DNS 失败和认证失败是不同层的问题，不能都归为“Redis 挂了”。

## 16. 自动化测试如何证明设计

| 测试 | 证明什么 |
| --- | --- |
| `RedisSessionIntegrationTest` | 登录会话真实写入 Redis，JDBC 会话表不再被使用 |
| `OperationsSummaryCacheIntegrationTest` | 同一请求复用缓存，键和 30～40 秒 TTL 正确 |
| `OperationsSummaryCacheInvalidationIntegrationTest` | 成功开户/转账在提交后清理，回滚不清理 |
| `OperationsSummaryCacheDegradationIntegrationTest` | 停止真实 Redis 后，总览查询回源 MySQL并产生指标 |
| `RedisCacheConfigurationTest` | TTL 抖动边界稳定且非法参数被拒绝 |
| `CacheFailureTelemetryTest` | get/put/evict/clear 四类错误均记录受控标签且不向上抛出 |
| `validate-redis-delivery.ps1` | Compose、Prometheus、Grafana、Helm 和 Runbook 的交付契约齐全 |

Testcontainers 在测试进程中启动真实 MySQL 8.4 和带密码的 Redis 7.4.10。MockMvc 则在 Spring MVC 与 Security 过滤链内发送 HTTP 请求。它们不是新的业务技术栈，而是让“代码能运行、会话真的共享、Redis 真故障时真的降级”有自动化证据。

## 17. 面试时的两分钟项目讲法

可以按以下逻辑组织，不需要背原句：

1. NorthLedger 是资金账户与转账平台，MySQL 通过事务、固定锁顺序、幂等键和双录流水保证资金一致性。
2. 为解决 Kubernetes 多副本会话共享和首页重复聚合读，引入 Redis，但严格限制为 Spring Session 与运行总览缓存，资金事实不进入 Redis。
3. 总览缓存使用 JSON、30 秒基础 TTL 加随机抖动；成功开户或转账在事务提交后清理，失败事务不清理。
4. 缓存故障回源 MySQL并记录低基数指标和结构化日志；会话依赖无法假降级，因此 readiness 摘流并通过 Redis Exporter、Prometheus 和 Alertmanager告警。
5. 本地 Compose 和 Minikube 用单节点实践 ACL、AOF、PVC、探针和 NetworkPolicy；生产 values 关闭内置状态组件，连接外部高可用 MySQL/Redis。
6. 用 Testcontainers、MockMvc、故障脚本、Grafana 看板和 Runbook 验证正常、失效、回滚、故障和恢复闭环。

## 18. 你现在应达到的学习层级

刚开始学习时，不需要先掌握 Redis Cluster、Sentinel、Lua、分布式锁和源码。先完成下面四级：

### 第一级：能解释

- 说清 MySQL 与 Redis 的数据边界。
- 说清缓存命中、未命中、TTL、失效和回源。
- 说清会话为什么要共享、为什么 Redis 故障影响登录。

### 第二级：能观察

- 能 PING、SCAN、TTL、GET 和阅读 INFO。
- 能在 Prometheus 查询 `redis_up`、内存和缓存错误。
- 能根据日志区分网络、认证、容量和应用错误。

### 第三级：能操作

- 能独立启动 Compose，登录并证明会话键与缓存键存在。
- 能执行开户/转账并证明缓存提交后失效。
- 能执行受控 Redis 故障演练并恢复服务。

### 第四级：能排障和复述

- 面对“登录掉线”“总览变慢”“Redis 内存高”“Grafana 无数据”，按链路收集证据。
- 不看文档画出浏览器、Nginx、API、MySQL、Redis、Exporter、Prometheus、Grafana、Alertmanager 的关系。
- 能诚实区分本地单节点实践和生产高可用边界。

完成第四级后，再学习 Redis Sentinel/Cluster 的选主、槽位和故障转移会更有抓手；它们不是当前项目简历成立的前置条件。

## 19. 最终自测题

1. 为什么账户余额不能照搬运行总览的 30 秒缓存策略？
2. `@Cacheable` 命中时，方法体还会执行吗？
3. 为什么清缓存必须放在 MySQL 事务提交后？
4. TTL 和主动失效分别解决什么问题？
5. Redis GET 失败与 Spring Session读取失败为何不能用同一种降级话术？
6. `noeviction` 达到内存上限后会发生什么，为什么项目仍选择它？
7. 为什么 Prometheus 标签不能包含缓存键、用户名或异常消息？
8. Redis Exporter、Prometheus、Grafana、Alertmanager 的数据流是什么？
9. Compose 的 `redis:6379` 与宿主机 `127.0.0.1:16379` 有什么区别？
10. 为什么 Minikube 单副本 Redis 不能在简历中描述为生产高可用？

如果以上问题都能结合代码、命令和观察结果回答，而不是只背定义，你才真正把这一部分变成了自己的项目能力。

## 20. 官方资料

- Spring Boot 3.5 缓存：https://docs.spring.io/spring-boot/3.5/reference/io/caching.html
- Spring Boot 3.5 Redis：https://docs.spring.io/spring-boot/3.5/how-to/nosql.html
- Spring Session 3.5 Redis：https://docs.spring.io/spring-session/reference/3.5/guides/boot-redis.html
- Spring Data Redis Cache：https://docs.spring.io/spring-data/redis/reference/3.5/redis/redis-cache.html
- Spring 事务事件：https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
- Redis 安全：https://redis.io/docs/latest/operate/oss_and_stack/management/security/
- Redis Exporter：https://github.com/oliver006/redis_exporter/blob/master/README.md
