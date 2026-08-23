# 认证、权限、会话与安全审计 Runbook

这份手册用于理解并排查 NorthLedger 的登录链路。目标不是背诵 Spring Security 类名，而是能够回答：用户如何证明身份、浏览器如何维持会话、服务端如何判断权限、修改请求为何需要 CSRF、一次失败如何留下证据。

## 1. 对象关系

```text
app_user ──1:N── app_user_role
    │
    ├─ BCrypt 密码哈希用于认证
    └─ 角色用于授权

浏览器 SESSION Cookie ──▶ SPRING_SESSION ──1:N── SPRING_SESSION_ATTRIBUTES

HTTP 请求 ──▶ CorrelationIdFilter ──▶ Spring Security ──▶ Controller
                    │                       │
                    └─ traceId              ├─ 401/403
                                            └─ audit_event + 认证指标
```

- `app_user` 是身份事实，保存用户名、显示名、状态和密码哈希。
- `app_user_role` 是职责事实，一个用户可以有多个角色。
- `SPRING_SESSION` 保存会话元数据，浏览器只持有随机会话标识，不持有角色真相。
- `audit_event` 保存操作证据，没有删除接口。
- `traceId` 把浏览器错误、应用日志和审计记录串到同一次请求。

## 2. 登录的完整流程

1. React 首次加载时请求 `GET /api/v1/auth/session`。
2. 服务端返回匿名会话，并写入前端可读的 `XSRF-TOKEN` Cookie。
3. 用户提交用户名和密码；前端读取该 Cookie，放入 `X-XSRF-TOKEN` 请求头。
4. Spring Security 先验证 CSRF，再由 `DatabaseUserDetailsService` 从 MySQL 读取用户。
5. BCrypt 使用数据库中的哈希校验密码，不解密、不比较明文存储。
6. 成功后执行会话固定攻击防护，创建新的会话标识并把会话保存到 MySQL。
7. 服务端写入登录成功审计，Micrometer 增加固定标签的成功计数。
8. React 再读取当前会话，得到显示名和角色，呈现相应页面。

失败登录统一返回 401 和稳定错误码，不告诉攻击者“用户名不存在”还是“密码错误”。失败同样写审计和指标，但密码、Cookie、完整请求体不会进入日志或数据库。

## 3. 认证、授权与 CSRF 的区别

- 认证回答“你是谁”。没有有效会话时，受保护接口返回 `401 Unauthorized`。
- 授权回答“你能做什么”。已经登录但角色不允许时返回 `403 Forbidden`。
- CSRF 回答“这个修改请求是否由获得页面令牌的同源客户端发出”。令牌缺失或错误时也返回 403，但错误码与普通越权不同。

前端隐藏菜单只是体验优化。真正的安全边界位于 `SecurityConfiguration`：即使用户手工拼接 URL 或直接调用接口，后端仍会检查角色。

## 4. 角色矩阵

| 操作 | ADMIN | OPERATOR | AUDITOR |
| --- | :---: | :---: | :---: |
| 查询账户、转账、流水和运行总览 | 是 | 是 | 是 |
| 创建账户 | 是 | 是 | 否 |
| 发起转账 | 是 | 是 | 否 |
| 查询安全审计 | 是 | 否 | 是 |
| 创建平台用户 | 是 | 否 | 否 |

`CUSTOMER` 只作为后续客户门户的预留角色。当前账户表还没有客户所有权字段，因此不会在缺少数据隔离的情况下提前授予全局账户访问权。

## 5. 首个管理员为什么由环境变量创建

全新数据库没有任何用户，如果又要求“管理员登录后才能创建用户”，就会形成启动死锁。`BootstrapAdministrator` 只在用户表为空且用户名、密码同时存在时创建一次管理员；数据库已有用户后不会覆盖密码或角色。

Compose 从本机 `.env` 注入，Linux 从权限受控的 EnvironmentFile 注入，Kubernetes 从 Secret 注入。源码和公开 values 不提供可用默认密码。

## 6. 正常观察方法

### 浏览器

- 登录成功后能看到用户显示名和角色对应菜单。
- 开发者工具的 Cookie 中有 `SESSION` 和 `XSRF-TOKEN`。
- `SESSION` 应为 HttpOnly，JavaScript 不能读取；`XSRF-TOKEN` 必须可读，前端才能写请求头。
- 修改请求应同时携带 `X-XSRF-TOKEN` 请求头。

### MySQL

```sql
SELECT username, display_name, status, created_at FROM app_user;
SELECT user_id, role FROM app_user_role ORDER BY user_id, role;
SELECT primary_id, session_id, creation_time, last_access_time, expiry_time FROM SPRING_SESSION;
SELECT event_type, outcome, actor, target_type, target_id, trace_id, source_ip, occurred_at
FROM audit_event
ORDER BY occurred_at DESC
LIMIT 20;
```

不要把 `password_hash` 复制到工单、截图或诊断包。它虽然不是明文密码，仍属于敏感认证数据。

### Prometheus

```promql
sum by (outcome) (increase(northledger_authentication_attempts_total[15m]))
```

该指标只使用 `success`、`failure` 等固定低基数标签，不使用用户名作为标签。单次事件去 MySQL 审计或日志查，趋势才去 Prometheus 查。

## 7. 常见故障与处理

### 登录页面返回 403 / CSRF 错误

观察：先确认 `GET /api/v1/auth/session` 是否成功且响应是否写入 `XSRF-TOKEN`；再确认登录 POST 请求头中的值与 Cookie 一致。

处理：不要关闭 CSRF。清理当前站点 Cookie 后刷新，让前端重新获取令牌；检查是否绕过 Nginx、跨域访问了另一个端口，或生产 HTTPS Cookie 配置与实际协议不一致。

### 正确密码仍返回 401

观察：查询用户是否存在、状态是否为 `ACTIVE`、是否有角色；查询最近登录失败审计，但不要打印提交的密码。

处理：确认连接的是预期数据库；首次空库检查引导管理员环境变量。若确需重置密码，应通过受控管理流程生成新 BCrypt 哈希，不直接改成明文。

### 登录后下一次请求又变成 401

观察：浏览器是否保存并发送 `SESSION`；`SPRING_SESSION` 是否存在对应记录；Nginx 是否把 Cookie 原样转发；数据库时间和会话过期时间是否合理。

处理：检查 Cookie 的 Domain、Path、SameSite 和 Secure。纯 HTTP 本地环境必须是 `SESSION_COOKIE_SECURE=false`，HTTPS 生产入口必须设为 `true`。

### 管理员接口返回 403

观察：请求 `GET /api/v1/auth/session` 查看当前角色，而不是只看页面文字；查询 `app_user_role`。

处理：确认登录的是目标账号并重新建立会话。角色变更后，已有会话可能仍保存旧认证信息，应退出并重新登录。

### 登录请求返回 429

观察：Nginx 只对同一来源地址的密码提交 POST 限制为每分钟 10 次，并允许 5 次短时突发；会话查询 GET 和退出 DELETE 不参与计数。

处理：先排除前端重复提交、健康检查误打认证路径或代理把所有用户来源地址合并。不要直接取消限流；若真实用户被代理汇聚，需要先正确配置受信代理和真实客户端地址。

### 审计事件有记录但日志找不到

观察：取审计事件的 `traceId`，在 Loki 或容器日志中检索；检查 Nginx 是否传递 `X-Request-ID`。

处理：确认应用使用结构化控制台日志且采集器正在读取相应容器。审计数据库是长期事实，日志是运行诊断，两者保留策略可以不同。

## 8. 复述验收

不看文档完成以下复述才算真正掌握：

1. 为什么 SESSION 必须 HttpOnly，而 XSRF-TOKEN 不能 HttpOnly？
2. 为什么“前端没有管理员菜单”不等于做了权限控制？
3. 401、403 和 CSRF 失败分别意味着什么？
4. 多个 API Pod 为什么还能共享登录状态？
5. 为什么 Prometheus 指标不能带用户名标签，而审计表可以保存操作者？
6. 首个管理员为什么只创建一次，为什么不能在源码写默认密码？

实操验收：以管理员、操作员、审计员分别登录；验证能做和不能做的操作；制造一次错误密码和一次越权；最后用前端、审计事件、Prometheus 指标和日志解释同一现象。
