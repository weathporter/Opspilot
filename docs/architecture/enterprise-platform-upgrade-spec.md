# 企业资金交易平台生产化升级规格

## 1. 产品定位

本项目建设一个可真实运行的企业资金账户与交易清结算平台，并围绕它完成 Linux、Docker、Kubernetes、MySQL、自动交付、可观测性和故障处置实践。平台首先是完整业务系统，运维能力通过部署环境、运行指标、告警、备份恢复、发布回滚和故障演练体现，而不是把前端改造成工具集合。

当前继续采用模块化单体。一个 Spring Boot 进程和一个 MySQL 数据库足以展示事务一致性、接口安全、数据迁移、监控和生产交付；只有出现独立扩缩容、独立发布或不同可用性目标时才评估服务拆分。

## 2. 第一阶段目标

第一阶段交付“身份、权限、会话和安全审计”闭环，解决当前演示用户写死在前端、业务接口完全匿名、操作无法追责的问题。

完成后必须满足：

1. 用户凭用户名和密码建立服务端会话，密码只保存安全哈希。
2. 会话保存到 MySQL，使多个 API 副本能够读取同一登录状态。
3. 所有业务接口默认需要登录，公开范围只保留健康、信息和 Prometheus 指标端点。
4. 管理员、业务操作员和审计员拥有不同权限；未登录返回 401，权限不足返回 403。
5. React 通过同源 Cookie 维持会话，并为所有非安全 HTTP 方法附加 CSRF 请求头。
6. 登录成功、登录失败和退出都写入不可变安全审计记录，同时生成低基数指标和结构化日志。
7. 不在代码、镜像、Compose 或 Helm 默认值中写入可用密码。首个管理员由部署环境变量一次性引导创建。
8. 现有账户、转账、双边流水、健康检查和监控能力继续可用。

## 3. 角色与最小权限

| 角色 | 允许行为 | 当前阶段明确不允许 |
| --- | --- | --- |
| `ADMIN` | 管理用户；继承业务操作和审计读取能力 | 绕过审计、读取密码哈希 |
| `OPERATOR` | 创建和查询账户；发起和查询转账；查看运行总览 | 管理用户、删除审计记录 |
| `AUDITOR` | 只读账户、转账、账务流水和安全审计 | 开户、发起转账、修改用户 |
| `CUSTOMER` | 为后续个人业务门户预留；本阶段只能建立会话和查看自身信息 | 读取全局账户、运行总览或安全审计 |

授权规则采用默认拒绝：规则没有明确允许的请求不能进入业务代码。后续把账户与客户身份关联后，再开放客户自己的账户、收款人和转账接口；本阶段不在缺少所有权约束的情况下提前授予 `CUSTOMER` 资金操作权限。

## 4. 模块边界

| 模块 | 职责 | 依赖边界 |
| --- | --- | --- |
| `identity` | 用户、角色、状态、密码认证和首个管理员引导 | 可写安全审计；不访问账户余额 |
| `security` | Spring Security 过滤链、CSRF、会话响应、401/403错误 | 调用 `identity` 和 `audit`；不实现业务规则 |
| `audit` | 不可变操作事件、查询接口和审计字段 | 不反向修改用户、账户或交易 |
| `account` | 账户生命周期、余额和状态 | 接收已经完成的身份授权结果 |
| `transfer` | 转账幂等、事务、双边流水 | 接收已经完成的身份授权结果 |
| `observability` | 请求关联 ID、指标和结构化日志 | 不保存业务事实，不记录密码或完整请求体 |

## 5. HTTP 契约

### 5.1 会话资源

| 方法和路径 | 权限 | 成功结果 | 失败结果 |
| --- | --- | --- | --- |
| `GET /api/v1/auth/session` | 公开 | 返回 `authenticated`、用户显示名和角色；匿名时 `authenticated=false` | 不返回内部异常 |
| `POST /api/v1/auth/session` | 公开但必须携带 CSRF | 使用表单字段 `username`、`password` 建立会话，返回当前会话 | 认证失败返回统一 401 ProblemDetail |
| `DELETE /api/v1/auth/session` | 已登录且必须携带 CSRF | 注销、删除服务端会话并返回 204 | 未登录返回 401 |

会话响应字段固定为：

```json
{
  "authenticated": true,
  "username": "operator",
  "displayName": "业务操作员",
  "roles": ["OPERATOR"]
}
```

角色采用有界枚举并按固定顺序输出，前端不能根据中文显示文本判断权限。

### 5.2 现有资源授权

| 接口 | 允许角色 |
| --- | --- |
| `POST /api/v1/accounts` | `OPERATOR`、`ADMIN` |
| `GET /api/v1/accounts/**` | `OPERATOR`、`AUDITOR`、`ADMIN` |
| `POST /api/v1/transfers` | `OPERATOR`、`ADMIN` |
| `GET /api/v1/transfers/**` | `OPERATOR`、`AUDITOR`、`ADMIN` |
| `GET /api/v1/operations/**` | `OPERATOR`、`AUDITOR`、`ADMIN` |
| `GET /api/v1/audit/events` | `AUDITOR`、`ADMIN` |

错误响应继续使用现有 RFC ProblemDetail 骨架，并稳定携带 `code`、`timestamp` 和可用时的 `traceId`。认证失败使用 `AUTHENTICATION_REQUIRED` 或 `INVALID_CREDENTIALS`，授权失败使用 `ACCESS_DENIED`。

## 6. 数据模型

### 6.1 用户

`app_user` 保存用户名、密码哈希、显示名、状态和时间戳。用户名唯一且创建后不作为随意修改的显示字段；密码哈希永不返回 API，也不写入日志。

`app_user_role` 保存用户与角色的多值关系，角色只能来自 `ADMIN`、`OPERATOR`、`AUDITOR`、`CUSTOMER`。

### 6.2 会话

`SPRING_SESSION` 与 `SPRING_SESSION_ATTRIBUTES` 使用 Spring Session JDBC 官方结构，由 Flyway 创建。应用配置禁止 Spring Session 自行建表，保证数据库结构只有一个版本事实来源。

### 6.3 安全审计

`audit_event` 至少保存：事件 ID、事件类型、结果、操作者、目标类型、目标标识、请求关联 ID、来源 IP、发生时间和受控描述。表不提供删除型业务接口。

第一阶段事件采用“稳定事件类型 + 结果”组合：

- `LOGIN` + `SUCCESS`/`FAILURE`
- `LOGOUT` + `SUCCESS`
- `ACCESS_DENIED` + `DENIED`
- `BOOTSTRAP_ADMIN_CREATED` + `SUCCESS`
- `USER_CREATED` + `SUCCESS`

审计记录不保存密码、Cookie、完整请求体或异常堆栈。

## 7. 威胁模型

| 威胁 | 具体滥用方式 | 第一阶段控制 |
| --- | --- | --- |
| 身份伪造 | 猜测或重放登录凭据 | 自适应密码哈希、统一失败信息、会话固定攻击防护 |
| 请求伪造 | 恶意站点借浏览器 Cookie 发起转账 | Spring Security CSRF Cookie 与请求头校验 |
| 越权 | 客户直接调用管理接口 | 服务端 URL 授权，默认拒绝 |
| 抵赖 | 操作者否认登录或退出 | 不可变审计事件、请求关联 ID、时间戳 |
| 信息泄露 | API 返回密码哈希或堆栈 | 专用响应 DTO、统一 ProblemDetail、日志字段白名单 |
| 拒绝服务 | 大量登录尝试 | 认证入口按来源地址限流，同时记录失败指标与审计证据 |
| 会话窃取 | JavaScript读取会话或明文网络截获 | HttpOnly、SameSite Cookie；生产环境 Secure 与 HTTPS |

## 8. 可观测性问题

值班人员必须能回答：

1. 最近登录成功率是否异常下降？
2. 失败集中在认证失败还是系统错误？
3. 某次登录或退出对应哪条审计记录和哪组日志？
4. 是否存在没有操作者或请求关联 ID 的安全事件？

对应信号：

- 指标 `northledger_authentication_attempts_total{outcome="success|failure"}`；标签只能来自固定集合。
- ECS JSON 日志中的 `event`、`outcome`、`username`、`traceId`；不记录密码。
- MySQL `audit_event` 作为长期业务审计事实。
- 登录失败率告警在积累真实基线后确定阈值，本阶段不拍脑袋制造寻呼噪声。

## 9. 部署配置

开发、Compose、Linux和Kubernetes都使用同一套应用代码：

- `BOOTSTRAP_ADMIN_USERNAME`：首个管理员用户名。
- `BOOTSTRAP_ADMIN_PASSWORD`：首个管理员初始密码，不提供默认值。
- `SESSION_COOKIE_SECURE`：HTTPS环境为 `true`，纯本地HTTP为 `false`。
- `LOGGING_STRUCTURED_FORMAT_CONSOLE`：容器和生产环境使用 `ecs`，IDE本地可留空使用可读文本。

管理员仅在用户表为空且两个引导参数同时存在时创建；参数缺一或密码不符合规则时应用拒绝启动。管理员已经存在时不覆盖密码和角色。

## 10. 验收标准

1. 新认证测试先因功能不存在而失败，实施后通过。
2. 未登录读取账户接口得到 401。
3. `CUSTOMER` 创建账户得到 403，`OPERATOR`得到 201。
4. 没有 CSRF 或携带错误 CSRF 的登录、开户和转账请求得到 403。
5. 有效用户登录后获得服务端会话，第二次请求无需再次提交密码。
6. 退出后旧会话不能继续访问受保护接口。
7. MySQL中能查询到成功、失败和退出审计记录，记录不含密码。
8. Prometheus能查询成功和失败认证计数，指标没有用户名标签。
9. 多个 API 副本连接同一 MySQL 时可以读取同一会话。
10. Maven测试、前端构建、Docker Compose配置校验和Helm渲染全部通过。

## 11. 官方实现依据

- Spring Security 6.5 用户名密码认证与 `AuthenticationManager`：https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/index.html
- Spring Security 请求授权：https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html
- Spring Security SPA CSRF：https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html#csrf-integration-javascript-spa
- Spring Security MockMvc CSRF测试：https://docs.spring.io/spring-security/reference/6.5/servlet/test/mockmvc/csrf.html
- Spring Session JDBC与Spring Boot：https://docs.spring.io/spring-session/reference/3.5/guides/boot-jdbc.html
- Spring Boot 3.5结构化日志：https://docs.spring.io/spring-boot/3.5/reference/features/logging.html#features.logging.structured
- Spring Boot 3.5 Testcontainers服务连接：https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html
