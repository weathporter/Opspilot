# NorthLedger 模块边界

NorthLedger 使用模块化单体：只有一个 Spring Boot 可执行单元，但代码按业务能力分区。账户与转账需要同一数据库事务保证一致性，当前没有拆分微服务所需的独立发布或独立扩缩容收益。

## 应用模块

| 模块 | 主要职责 | 明确不承担 |
| --- | --- | --- |
| `identity` | 用户、角色、状态、密码哈希、首个管理员和用户管理 | 账户余额、交易事务 |
| `security` | 认证、授权、会话响应、CSRF、401/403 和安全事件入口 | 资金业务规则 |
| `audit` | 不可变审计事件、受控查询 DTO | 修改或删除历史证据 |
| `account` | 开户、账户查询、余额和状态 | 身份认证、发布操作 |
| `transfer` | 转账幂等、锁顺序、事务一致性、订单和双录流水 | 用户管理、监控采集 |
| `dashboard` | 聚合账户、交易、流水和运行状态的只读模型 | 写资金数据 |
| `observability` | 请求关联 ID、认证指标和日志上下文 | 保存业务事实或敏感请求体 |
| `common` | ProblemDetail、业务异常和跨模块通用类型 | 具体领域规则 |
| `config` | Spring Boot 技术配置 | 业务流程编排 |

## 数据所有权

| 表 | 所属模块 | 关键约束 |
| --- | --- | --- |
| `account` | account | 账号唯一、余额使用精确十进制 |
| `transfer_order` | transfer | 客户端请求号唯一，保存处理结果 |
| `ledger_entry` | transfer | 每笔成功订单产生借方与贷方流水 |
| `app_user` / `app_user_role` | identity | 用户名唯一、密码只存 BCrypt 哈希 |
| `audit_event` | audit | 只追加、无删除业务接口 |

Redis 不拥有任何资金业务事实：`northledger:session:*` 由 `security/session` 保存可过期的共享登录态；`northledger:cache:*` 由 `dashboard` 保存可从 MySQL 重建的运行总览。Flyway V2 已创建的 `SPRING_SESSION*` 表作为历史兼容结构保留，0.3.0 运行时不再读写，避免修改已经执行过的迁移。

其他模块不能绕过服务层随意修改不属于自己的表。数据库结构由 Flyway 单向演进：已经执行的 `V1` 永不修改，新增结构使用 `V2+`。

## 接口边界

- Controller 只处理 HTTP 映射、校验和状态码，不直接编写事务规则。
- Application Service 管理用例与事务边界。
- Repository 负责持久化查询，不向前端暴露实体。
- API 使用专用请求/响应 DTO，密码哈希和内部异常不进入响应。
- 前端类型与 REST 契约一致，但不能作为服务端权限来源。

## 工程目录

| 目录 | 职责 |
| --- | --- |
| `src/` | Java 应用、数据库迁移和自动化测试 |
| `frontend/` | React/TypeScript 业务门户 |
| `deploy/linux/` | Rocky Linux、systemd、Nginx 和日志轮转 |
| `deploy/docker/` | 容器入口和 Nginx 配置 |
| `deploy/k8s/` | Helm 与三节点实验环境 |
| `observability/` | 指标、看板、告警和日志采集配置 |
| `scripts/` | 构建、发布、回滚、巡检、备份和故障演练 |
| `docs/` | 架构、Runbook、学习路线和复盘 |

## 企业级演进触发条件

不创建只有目录没有行为的空模块。只有出现以下证据才演进：

- 独立扩缩容、团队边界或发布节奏差异明显时，评估服务拆分。
- 运行总览已经出现跨多张表的重复聚合读，因此 0.3.0 只为该读模型和共享会话引入 Redis；新增缓存对象仍需先证明收益、TTL 和失效策略。
- 出现跨系统可靠事件分发需求时，评估 Outbox 与消息队列。
- 出现跨进程慢调用且日志/指标不足以定位时，评估 OpenTelemetry tracing。
- 出现真实知识检索业务时，才评估向量数据库；当前资金交易平台不需要 Milvus。
