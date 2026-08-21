> [返回导航](../README.md) · [打开完整合订本](../OpsPilot-全景百科与实战学习手册.md)

# 第四篇：真正的业务核心——Java、Spring Boot 与 MySQL

## 第 14 章　Spring Boot 应用怎样启动并连接各层

### 14.1 从 main 方法开始

`OpsPilotApplication` 是 Java 进程入口。`SpringApplication.run` 创建应用上下文，扫描组件，完成自动配置，启动内嵌 Tomcat，并把 Controller、Service、Repository、Filter 等 Bean 连接起来。

启动日志通常经历：读取配置 -> 初始化数据源 -> Flyway 校验/迁移 -> JPA 校验实体映射 -> Tomcat 监听端口 -> 应用可用状态变化。启动失败时应找最早的根因异常，最后一行往往只是外层包装。

### 14.2 Maven 依赖各自解决什么

- `spring-boot-starter-web`：Spring MVC、JSON、嵌入式 Tomcat。
- `spring-boot-starter-validation`：请求字段校验。
- `spring-boot-starter-data-jpa`：实体、仓储、Hibernate 与事务集成。
- `spring-boot-starter-actuator`：健康、信息和指标端点。
- `micrometer-registry-prometheus`：把 Micrometer 指标导出为 Prometheus 格式。
- `flyway-core/flyway-mysql`：数据库结构迁移。
- `mysql-connector-j`：JDBC 驱动。
- 测试依赖：JUnit、MockMvc、Spring Test、Testcontainers MySQL。

Starter 是一组协调版本的依赖集合，不是某个神秘框架。出现冲突时使用 `mvn dependency:tree` 观察实际解析版本。

### 14.3 配置优先级与环境变量

`application.yml` 使用 `${ENV_NAME:default}`。IDE、本地 JAR、Compose、systemd、Kubernetes 可以运行同一个制品，只通过外部配置改变数据库地址、端口、环境和版本。

关键配置：

- 默认应用端口 18080，避开常见 8080。
- `DB_URL` 默认本机 3306；Compose 覆盖为 `mysql:3306`。
- `open-in-view=false`，查询在服务事务内完成，避免 Web 层隐式懒加载。
- `ddl-auto=validate`，Hibernate 只验证，不负责改表。
- Flyway 是结构演进的唯一事实来源。
- 只暴露 health、info、prometheus 三类 Actuator 端点。
- 优雅停机与请求 ID 日志格式启用。

默认开发密码只是方便本地学习，不能作为生产秘密管理方案。

### 14.4 依赖注入

Controller 构造器接收 Service，Service 构造器接收 Repository 和 Clock。Spring 容器在启动时创建并注入依赖。

构造器注入的优点：依赖明确、字段可保持不可变、测试中容易替换、对象创建后立即完整。它不是“为了少写 new”，而是把对象装配交给容器，同时保留类本身的职责边界。

### 14.5 Clock 为什么也要注入

直接在业务各处调用系统当前时间，会让测试结果随运行时刻变化。项目注入 `Clock`，测试可固定时间；写入前还截断到微秒，以匹配 MySQL `TIMESTAMP(6)`，避免 Java 纳秒值与数据库回读值不相等。

这体现一个通用原则：时间、随机数、外部网络等不稳定输入应有可替换边界。

### 14.6 启动失败排查

1. Java 是否为 17。
2. 环境变量是否被当前启动方式加载。
3. 端口是否被占用。
4. JDBC 地址、DNS、3306、用户名密码和权限。
5. Flyway history 与迁移校验和。
6. 实体与表结构是否匹配。
7. 只读文件系统和日志目录权限。
8. 内存和进程退出原因。

## 第 15 章　HTTP 分层：Filter、Controller、DTO、Service、Repository

### 15.1 一次请求的完整 Java 路径

以创建账户为例：

```text
HTTP POST /api/v1/accounts
 -> CorrelationIdFilter
 -> AccountController.create
 -> CreateAccountRequest + @Valid
 -> AccountApplicationService.create + @Transactional
 -> Account.open
 -> AccountRepository.saveAndFlush
 -> Hibernate/JDBC
 -> MySQL account
 -> AccountResponse
 -> JSON + HTTP 201
```

每层都只承担自己的职责。你必须能在 IDE 中逐步打断点，而不是只记这个箭头图。

### 15.2 Filter：请求关联 ID

`CorrelationIdFilter` 最先处理请求。若上游 `X-Request-ID` 符合 1 至 64 位安全字符规则，就继续使用；否则生成 UUID。它把 ID 写入 MDC 和响应头，日志格式再从 MDC 读取。

为什么过滤用户输入？如果允许换行等字符直接进入日志，攻击者可能伪造多行记录。为什么 finally 中清理 MDC？Tomcat 会复用线程，不清理会让下一个请求继承错误 traceId。

这不是分布式链路追踪。它只能在当前 HTTP 请求和代理传递范围内关联日志，没有 span、跨异步消息上下文和采样模型。未来引入 OpenTelemetry 后可把它与标准 trace/span 体系衔接。

### 15.3 Controller：协议适配器

Controller 负责 URL、HTTP 方法、请求头、JSON、参数绑定和状态码。它不直接访问 Repository，也不写余额规则。

这样设计的原因：HTTP 只是业务的一种入口。如果规则散落在 Controller，批处理、消息消费或测试直接调用服务时就可能绕过规则。

### 15.4 DTO 与实体不能混用

请求 DTO 表达外部输入契约；响应 DTO 表达对外输出；实体表达持久化状态和领域行为。

直接返回 JPA 实体可能暴露内部主键、版本字段、延迟加载关系，并让数据库模型绑死 API。OpsPilot 的 `AccountResponse`、`TransferResponse` 等在事务内把实体转换为只读快照。

### 15.5 Bean Validation

开户账号要求 8 至 32 位数字，姓名非空且最多 64 字符，余额非负且最多两位小数；转账金额至少 0.01，付款和收款账号格式合法。

前端的 `required`、`min`、`pattern` 只改善用户体验，不能保护服务端，因为调用者可以绕过网页直接发 HTTP。后端校验是可信边界；数据库 CHECK/UNIQUE 又构成最终纵深防线。

### 15.6 Service：用例编排与事务边界

ApplicationService 组合实体、仓储、时间和异常，定义一次用例的事务边界。`@Transactional(readOnly=true)` 表达只读意图，减少不必要的脏检查，但它不是数据库绝对只读安全策略。

事务注解通常依赖 Spring 代理。类内部自调用、非代理对象或异常被吞掉时可能不按预期生效；学习阶段需要知道边界，不必现在深入所有代理实现。

### 15.7 Repository：持久化访问边界

Repository 继承 `JpaRepository`，Spring Data 根据方法名或 JPQL 生成实现。分页参数下推到数据库，避免把全表读进 JVM 后截断。

仓储不是“数据库表的 Controller”。它只负责查询和持久化，不承担 HTTP 或部署职责。

## 第 16 章　REST API 与统一错误契约

### 16.1 现有接口清单

| 方法与路径 | 功能 | 成功结果 |
| --- | --- | --- |
| `POST /api/v1/accounts` | 创建账户 | 201 + 账户快照 |
| `GET /api/v1/accounts` | 搜索/列出最近账户 | 200 + 数组 |
| `GET /api/v1/accounts/{accountNo}` | 查询单账户 | 200 或 404 |
| `POST /api/v1/transfers` | 发起/重放转账 | 201 + 订单快照 |
| `GET /api/v1/transfers` | 最近订单和状态筛选 | 200 + 数组 |
| `GET /api/v1/transfers/{requestId}` | 按幂等键查最终状态 | 200 或 404 |
| `GET /api/v1/transfers/{requestId}/ledger` | 订单、两条流水和平衡结果 | 200 或 404 |
| `GET /api/v1/operations/summary` | 运维总览聚合 | 200 + 汇总与趋势 |
| `GET /actuator/health/readiness` | 就绪状态 | 200/非就绪状态 |

当前没有 OpenAPI/Swagger 依赖，Prometheus 也不是接口文档。若要面向协作团队，可补 Springdoc/OpenAPI，但在补之前不能告诉面试官“已提供 Swagger 文档”。

### 16.2 状态码语义

- 400：JSON/字段校验失败。
- 404：账户或订单不存在。
- 409：账号唯一冲突，或同一幂等键被不同载荷复用。
- 422：格式正确但违反业务规则，如余额不足、同账户转账、幂等键为空。
- 500：未预期服务端错误；当前统一处理器没有自定义兜底处理，Spring 会处理。

把所有失败都返回 200 再在 JSON 中放错误码，会破坏 HTTP 基础语义；把业务冲突都当 500，又会污染错误率监控。

### 16.3 ProblemDetail

统一异常处理器把业务异常转换为标准化问题响应，包含 HTTP 状态、稳定 `code`、人类可读 `detail`、时间和 traceId。稳定错误码给程序判断，中文详情给用户和运维理解。

不要把堆栈、SQL、密码或内部路径直接返回客户端。详细异常留在服务端日志，通过 traceId 关联。

### 16.4 限制无界查询

账户和订单列表将 limit 收口在 1 至 100。即使前端只请求 100，服务端也必须保护自己，防止其他调用方传入巨量数字拖慢数据库和网络。

当前使用“最新前 N 条”，未实现完整分页游标。数据规模增长后应增加稳定排序键、分页响应和必要索引，而不是只把上限改大。

## 第 17 章　MySQL 数据模型：账户、订单和流水

### 17.1 三张表解决三个不同问题

`account` 保存账户当前状态；`transfer_order` 保存一次转账业务请求和幂等记录；`ledger_entry` 保存余额为什么变化的审计证据。

只保存当前余额无法回答历史变化；只保存流水每次计算当前余额成本高且并发设计更复杂。当前模型用账户快照 + 不可变流水兼顾读取与审计。

### 17.2 account 表

关键字段：

- `id`：内部自增主键。
- `account_no`：外部业务账号，唯一。
- `holder_name`：账户名称。
- `balance DECIMAL(19,2)`：精确十进制余额。
- `status`：ACTIVE/FROZEN/CLOSED 字符串。
- `version`：JPA 乐观锁版本。
- 创建和更新时间：`TIMESTAMP(6)`。

数据库约束：账号唯一、余额不得小于零。

为什么不用 double 存金额？二进制浮点不能精确表示很多十进制小数，累积计算可能产生误差。Java 使用 BigDecimal，MySQL 使用 DECIMAL，并用 compareTo 比较数值而非受 scale 影响的 equals。

### 17.3 transfer_order 表

`request_id` 来自 `Idempotency-Key` 并唯一；同时保存付款账号、收款账号、金额、状态和时间。保存原始业务载荷的目的，是在重试时判断“同一键同一请求”还是“同一键被错误地用于另一请求”。

当前状态只有 PROCESSING 和 COMPLETED。因为订单创建、余额和流水处于同一事务，失败会整体回滚，不留下 FAILED 订单。生产系统若需要记录失败原因和全生命周期，通常会把请求接收、业务执行、失败审计设计成更复杂的状态机或独立审计渠道。

### 17.4 ledger_entry 表

一笔成功转账写两行：付款方 DEBIT、收款方 CREDIT。金额始终为正，方向由 entry_type 表达；`balance_after` 保存交易后余额；外键关联订单。

审计接口根据方向反推交易前余额：DEBIT 的 before = after + amount；CREDIT 的 before = after - amount。

`balanced=true` 的当前判断是正好两条流水且借贷金额相等。它证明这笔订单的双边记录在本模型中平衡，不等于完成全库总账、外部渠道对账或会计科目级审计。

### 17.5 主键、业务键、唯一约束

自增 id 便于内部关联；account_no/request_id 是业务标识。应用层先查是否存在不能消除并发竞态：两个请求可同时查到“不存在”再插入。数据库唯一约束是最终防线。

开户使用 `saveAndFlush` 立即发送 INSERT，让唯一冲突在当前 try/catch 内发生并转换为 409，而不是延迟到事务提交后难以在局部处理。

### 17.6 索引与最左前缀

V1 建立：

- `(account_no, created_at)` 支持按账户和时间查流水。
- `(source_account_no, created_at)` 支持按付款账户和时间查订单。
- 唯一约束通常也带来唯一索引。

复合索引按从左到右的前缀使用。索引不是越多越好：会占空间，INSERT/UPDATE 时维护，统计信息和选择性也影响是否采用。应由真实查询、执行计划和数据规模决定。

当前前端主要按最近时间和 request_id 查询，未来需要结合慢查询与 `EXPLAIN` 评估是否增加 created_at 等索引。不能只根据字段“看起来常用”随意加。

### 17.7 Flyway 迁移

应用启动时 Flyway 读取 `flyway_schema_history`，校验已执行版本的校验和，再按顺序执行新脚本。Hibernate 设置 `ddl-auto=validate`，不会偷偷改表。

已经在持久化/共享环境执行过的 V1 永远不再修改，包括只加注释，因为校验和会变化。任何结构变更创建 V2、V3……并考虑向前/向后兼容、数据回填和回滚策略。

常见 `Migration checksum mismatch` 处理原则：先确认文件是否被错误修改、环境是否执行过、是否存在恶意/意外漂移。不要未经分析就 repair；repair 会更新历史认定，可能掩盖真正的结构差异。

### 17.8 直接查看数据库

Compose 中可进入 MySQL 或从主机 3307 连接：

```sql
USE opspilot;
SHOW TABLES;
DESCRIBE account;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
SELECT account_no, holder_name, balance, status, version FROM account;
SELECT request_id, source_account_no, target_account_no, amount, status FROM transfer_order;
SELECT transfer_order_id, account_no, entry_type, amount, balance_after FROM ledger_entry;
```

不要在没有 WHERE、备份和确认的情况下对共享库执行 UPDATE/DELETE。学习查询使用应用账号；结构/管理操作使用受控管理账号，不让应用使用 root。

## 第 18 章　事务：为什么四类写入要么全成、要么全败

### 18.1 事务的直观意义

转账不是四个互不相关的 SQL。业务要求在一个一致性边界内完成：扣款、入账、订单、两条流水。若扣款提交后入账失败，资金凭空减少；若余额变化但流水缺失，审计无法解释。

`@Transactional` 让这些数据库操作使用同一事务。方法正常结束时提交，未捕获运行时异常时回滚。

### 18.2 ACID 用 OpsPilot 解释

- 原子性 Atomicity：四类写入作为整体提交或回滚。
- 一致性 Consistency：约束和业务规则使提交前后满足余额非负、订单唯一、流水有关联等条件。数据库不会自动理解所有业务一致性，应用也要负责。
- 隔离性 Isolation：并发事务不会任意看见彼此未提交中间状态；具体行为由隔离级别和锁决定。
- 持久性 Durability：提交后由 MySQL 日志和存储机制保证重启后仍存在，但这不等于零丢失灾备，仍受刷盘、复制和硬件策略影响。

### 18.3 回滚测试

测试创建余额 100 的付款账户，尝试转 200。实体 `debit` 抛出 `BusinessRuleException`，整个事务回滚。测试断言：两个账户余额不变、订单数为 0、流水数为 0。

这比只断言“抛出了异常”更强，因为它验证失败没有留下数据副作用。

### 18.4 异常与回滚边界

Spring 默认对未检查异常回滚。若捕获异常后不再抛出，外层事务可能仍提交；若抛检查异常而未配置，也可能不按初学者预期回滚。OpsPilot 的业务异常继承运行时异常，保持统一回滚语义。

事务注解经代理生效：同一类内部方法直接调用、私有方法、手工 new 出来的对象都可能绕过代理。面试不必背所有细节，但要知道“写了注解”还需要正确调用边界。

### 18.5 隔离级别与锁的关系

MySQL InnoDB 默认常见隔离级别为 REPEATABLE READ，但实际环境可配置。隔离级别解决读现象，账户扣款仍需要明确的并发更新策略。项目使用 `SELECT ... FOR UPDATE` 悲观写锁，在事务结束前锁住账户行。

不能只说“有事务就不会并发错”。两个事务都在事务里，也可能基于同一个旧余额做判断；必须结合锁、条件更新或乐观并发控制。

## 第 19 章　转账算法：幂等、悲观锁、死锁与双边流水

### 19.1 为什么幂等是金融接口的基本要求

客户端发送转账后可能在响应返回前超时。它不知道服务端到底没执行、正在执行还是已经完成，只能重试。如果每次 POST 都重新扣款，会重复转账。

幂等键表示“这几次重试属于同一个业务意图”。客户端重试必须复用原键；新的业务请求使用新键。

### 19.2 完整算法顺序

1. 校验幂等键非空且不超过 64 字符。
2. 拒绝付款与收款账号相同。
3. 无锁快速查询 request_id；若存在，比较载荷并重放旧结果。
4. 把两个账号按字典序排序。
5. 按固定顺序对两个账户执行悲观写锁查询。
6. 锁后再次查询 request_id，关闭并发窗口。
7. 映射回真实付款方和收款方。
8. 获取统一微秒时间。
9. 付款方扣款并检查状态/余额；收款方入账。
10. 创建转账订单。
11. 创建 DEBIT/CREDIT 两条流水和余额快照。
12. 把订单状态推进到 COMPLETED。
13. 事务提交；JPA 脏检查生成 UPDATE。

### 19.3 为什么先快查，再锁后双检

多数重放发生在原请求已经完成后。第一次查询能直接返回旧结果，不需要再争抢账户锁。

但两个相同幂等键的请求可能同时到达，都在第一次查询时没看到订单。只有获得账户锁后再次检查，后来的请求才能看到前一个已提交订单并重放。

数据库 request_id 唯一约束仍是最终防线。多层保护不是重复浪费：快速路径解决性能，锁后双检关闭主要竞态，唯一约束防御遗漏和其他写入口。

### 19.4 同一键不同载荷为什么返回冲突

若 request-id-1 第一次表示 A 向 B 转 100，后来却表示 A 向 C 转 500，服务不能猜测哪个意图正确，也不能静默返回旧订单。项目比较付款、收款和金额；不一致返回 409 `IDEMPOTENCY_KEY_CONFLICT`。

金额用 `compareTo`，所以 10.0 与 10.00 按数值相等。

### 19.5 悲观锁怎样防止超扣

`findByAccountNoForUpdate` 在事务中形成 `SELECT ... FOR UPDATE`。一个事务锁住付款账户后，另一个修改同一账户的事务等待；先提交后，后来者读取最新余额再判断。

悲观锁适合冲突概率较高、业务不能容忍重试逻辑复杂的关键余额更新；代价是等待、锁竞争和潜在死锁。低冲突场景可用乐观锁 + 重试，或数据库条件更新。

### 19.6 为什么固定账号顺序能降低死锁

假设事务一 A->B 先锁 A 再等 B；事务二 B->A 先锁 B 再等 A，两者形成环形等待。项目不按付款/收款角色加锁，而是两个账号字典序小的先锁、大的后锁。所有事务遵守同一全局顺序，破坏环形等待条件。

这能显著降低这类死锁，不代表数据库中永远不会出现任何死锁。其他表、索引范围和外部事务顺序仍可能产生死锁；生产代码还需识别死锁错误并进行有限、带退避的重试。

### 19.7 `@Version` 与悲观锁为什么同时存在

Account 实体还有 `@Version` 乐观锁字段。转账主路径明确使用悲观锁；version 为其他可能的账户更新入口提供额外并发检测。

不要含糊地说项目“同时靠两种锁保证同一件事”。应说明主路径依赖 `FOR UPDATE`，version 是实体级额外保护；若未来所有写入口明确统一，需要评估是否简化。

### 19.8 锁等待与死锁排查

现象可能是接口延迟升高、504、事务回滚或数据库死锁日志。检查当前事务、锁等待、慢 SQL、连接池和调用堆栈。不要只把超时时间调大。

需要理解的 MySQL 工具包括 `SHOW ENGINE INNODB STATUS`、Performance Schema 锁表和慢查询日志；具体权限和版本视环境而定。实验必须在隔离库，用两个会话手工锁相同行，观察第二会话等待，再提交/回滚释放。

### 19.9 当前并发验证边界

代码实现了固定顺序悲观锁、锁后幂等双检和唯一约束，已有集成测试验证顺序重放、回滚和载荷冲突。但当前测试集没有真正启动多个线程同时轰击同一账户/幂等键，也没有性能压测数据。

因此简历可以说“设计并实现并发控制与幂等机制，并用真实 MySQL 集成测试验证重放和回滚”，不能说“已通过高并发压测”或给出虚构 TPS。

下一步应补：并发集成测试、死锁/锁等待指标、JMeter/k6 压测基线、数据库连接池与资源曲线。先建立小规模可重复实验，不追求夸张数字。

### 19.10 转账源码逐段调试练习

在 IDEA 中：

1. 以可连接 MySQL 的配置启动应用。
2. 在 `TransferController.transfer`、`TransferApplicationService.transfer`、`Account.debit`、`LedgerEntry.debit` 设置断点。
3. 创建两账户并发转账请求。
4. 观察 requestId、排序后的 first/second、source/target 映射、余额前后、订单状态。
5. 提交后在 MySQL 查三张表。
6. 使用同一键重放，确认断点走快速 replay 而不再 debit。
7. 用同一键改金额，确认 409。
8. 发起余额不足，确认数据库没有订单和流水。

### 19.11 面试复述模板

“转账接口把扣款、入账、订单和双边流水放在一个 MySQL 事务里。客户端用 Idempotency-Key 表达一次业务意图；服务先走无锁快查，再按账号固定顺序悲观锁住两个账户，锁后再次检查幂等记录，最后由 request_id 唯一约束兜底。固定锁顺序降低 A->B/B->A 的死锁风险；同键同载荷返回旧结果，同键不同载荷返回 409。余额不足抛运行时业务异常，事务整体回滚。当前验证了真实 MySQL 重放和回滚，但尚未宣称完成高并发压测。”

## 第 20 章　JPA/Hibernate：项目里实际发生了什么

### 20.1 实体映射

`@Entity` 把 Java 类映射到表，`@Column` 描述列，`@Id/@GeneratedValue` 描述主键，`@Enumerated(EnumType.STRING)` 以可读字符串保存枚举。实体需要无参构造器供 JPA 反射，但项目把它设为 protected，业务代码通过工厂方法创建完整对象。

### 20.2 持久化上下文与脏检查

事务内从 Repository 加载的实体处于受管理状态。调用 `source.debit` 和 `target.credit` 改字段后，即使没有显式 save，事务提交前 Hibernate 会比较变化并生成 UPDATE，这叫脏检查。

订单 `save` 后再 `complete`，状态变化同样由脏检查更新。理解这一点能解释“为什么代码没有每处都写 update SQL”。

### 20.3 LAZY 与 Open Session in View

流水到订单是 LAZY 关联。项目关闭 Open Session in View，并在服务事务内加载和转换 DTO，避免 Controller 返回 JSON 时才触发额外查询或 LazyInitializationException。

LAZY 不等于永远不会查询；访问关联时仍会加载。生产性能排查要关注 N+1 查询、抓取计划和 SQL 日志。

### 20.4 自动方法名查询与 JPQL

`findByAccountNo`、`findAllByStatusOrderByCreatedAtDesc` 等由方法名解析；汇总余额和金额使用 JPQL。选择方法名还是显式查询，取决于复杂度和可读性，不能把超长条件强塞进方法名。

### 20.5 DTO 映射和数据精度

实体不会直接出 Web 层。DTO 还可计算展示字段，例如流水响应根据方向反推 balanceBefore。时间写入前截断微秒，金额全链使用 BigDecimal/DECIMAL。

前端 TypeScript 当前把 amount/balance 声明为 number。浏览器对演示金额足够，但真正大额金融系统不应无条件用 IEEE 754 number 承载高精度金额，通常使用字符串或专用十进制库。这是当前个人项目的边界。

## 第 21 章　运维总览读模型：真实数据，不是静态大屏

### 21.1 为什么单独有 dashboard 模块

总览需要跨账户和转账汇总，但它只读、不修改资金。放在独立 dashboard 包可以避免 account/transfer 为展示互相污染职责。

### 21.2 汇总口径

接口返回生成时间、环境、版本、账户数、余额总和、订单总数、完成数、完成金额、成功率、24 小时趋势和最近 6 笔订单。

所有数字来自 MySQL 查询。前端不会用当前表格的有限行数反推总数，也不写死虚构延迟和备份体积。

### 21.3 24 小时零值桶

服务先建立最近 24 个整点的零值桶，再把订单归入对应小时。没有交易的小时也返回 0，前端趋势线连续。

当前实现会把 24 小时订单列表读入应用聚合；数据规模增长后，应考虑数据库按小时 group by、预聚合、缓存或时序/分析系统。但个人项目阶段这样更易读、数据量可控。

### 21.4 “成功率”边界

当前成功率 = COMPLETED 订单 / 所有已落库订单。因为失败事务不留下 FAILED 订单，在正常模型下它很可能长期接近 100%。它不能代表所有请求成功率，因为参数错误、余额不足、5xx 不一定落入 transfer_order。

更完整的业务成功率应结合 HTTP 指标、失败审计或显式失败状态。面试时要主动指出这个口径边界，而不是把 100% 当成生产成绩。

## 第 22 章　后端测试：怎样证明，而不是怎样自信

### 22.1 测试金字塔在当前项目中的落点

项目包含小型 Filter 测试、MockMvc 健康检查、真实 MySQL 集成测试和浏览器端到端测试。数量不大，但覆盖关键风险。

### 22.2 为什么不用 H2

转账依赖 MySQL 的锁、唯一约束、CHECK、时间精度和 Flyway 脚本。H2 的 SQL 方言与并发行为可能不同，测试通过不代表 MySQL 正确。Testcontainers 在测试时启动真实 MySQL 8.4，让迁移、JPA 和业务使用同类数据库。

代价是测试需要 Docker、启动更慢、环境故障更多。单元测试仍可对纯业务方法快速验证；关键数据库语义用 Testcontainers。

### 22.3 当前七项测试证明什么

- readiness 端点可用。
- 合法请求 ID 被保留，不安全 ID 被替换。
- 相同幂等键重放只产生一张订单、两条流水和一次余额变化。
- 余额不足整体回滚。
- 同键不同载荷被拒绝。
- 账户、订单、流水和运维总览读模型来自真实 MySQL，并验证趋势、总额和 balanced。

### 22.4 它们没有证明什么

- 没有真实多线程并发冲突测试。
- 没有负载/容量/长稳测试。
- 没有认证授权测试，因为功能未实现。
- 没有外部 MySQL 断网、主从切换和恢复点目标测试。
- 没有完整浏览器兼容矩阵。

### 22.5 构建与测试命令

```bash
mvn test
mvn package
mvn -DskipTests package
mvn dependency:tree
```

`package -DskipTests` 只证明跳过测试后能打包，不是正式验收。发布门禁应在同一代码版本上先运行测试，再构建不可变制品。

### 22.6 测试失败怎样排查

1. Docker Engine/Testcontainers 是否可用。
2. 容器启动和镜像拉取日志。
3. Flyway 迁移和实体校验。
4. 测试数据是否清理、唯一值是否冲突。
5. 时间精度和时区。
6. 断言是业务失败还是环境失败。

不要通过放宽所有断言“修复”测试，也不要把偶发环境失败误判为业务正确。

## 第 23 章　后端核心实验与验收

### 23.1 开户实验

正常：创建合法账号，观察 HTTP 201、响应 X-Request-ID、account 表一行、version 初值和时间。

异常一：相同账号再次创建，预期 409，数据库仍一行。

异常二：账号含字母或长度不足，预期 400，事务不进入数据库写入。

### 23.2 转账实验

正常：A=1000，B=100，转 250，预期 A=750、B=350、一订单、两流水、balanced=true，总余额仍 1100。

重放：同键同载荷再发，结果相同，余额和行数不再变化。

冲突：同键改金额，预期 409。

余额不足：预期 422，余额、订单和流水不变。

同账户：预期 422，不进入锁和写入。

### 23.3 数据一致性手工校验

```sql
SELECT SUM(balance) FROM account;
SELECT request_id, COUNT(*) FROM transfer_order GROUP BY request_id HAVING COUNT(*) > 1;
SELECT transfer_order_id,
       SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END) AS debit,
       SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END) AS credit,
       COUNT(*) AS entries
FROM ledger_entry
GROUP BY transfer_order_id;
```

这些查询是学习校验，不是完整会计对账系统。生产还需考虑期初、冲正、手续费、币种、冻结、日切和外部渠道。

### 23.4 必须能够回答

1. Controller、Service、Repository、Entity、DTO 各做什么？
2. 为什么前后端都校验，数据库还要约束？
3. 为什么 Flyway V1 不能改？
4. 为什么金额不用 double？
5. `@Transactional` 能自动解决所有并发问题吗？
6. 幂等快查、锁后双检、唯一约束分别解决什么？
7. 固定锁顺序为什么只是降低而非消灭所有死锁？
8. 当前成功率为什么不代表所有转账请求的成功率？
9. Testcontainers 比 H2 更真实在哪里，代价是什么？
10. 当前没有并发压测，你的简历措辞怎样保持诚实？
