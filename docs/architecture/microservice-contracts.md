# NorthLedger 微服务接口与数据契约

> 此表以改造前的 Java Controller、SecurityConfiguration 和前端 API 调用为基线。标记“拟归属”的服务尚需通过代码与集群实测确认。

## 浏览器接口（URL 与 JSON 保持兼容）

| 方法与路径 | 当前角色要求 | 成功结果 | 拟归属 |
| --- | --- | --- | --- |
| `GET /api/v1/auth/session` | 匿名可读 | `AuthSessionResponse`，刷新 XSRF-TOKEN | Access |
| `POST /api/v1/auth/session` | 匿名可提交，必须 CSRF | `AuthSessionResponse`，设置 HttpOnly SESSION | Access |
| `DELETE /api/v1/auth/session` | 已登录，必须 CSRF | 匿名 `AuthSessionResponse` | Access |
| `GET /api/v1/admin/users` | ADMIN | 用户数组，不含密码哈希 | Access |
| `POST /api/v1/admin/users` | ADMIN，必须 CSRF | 201 + 用户对象 | Access |
| `GET /api/v1/audit/events` | ADMIN/AUDITOR | 安全审计事件数组 | Access |
| `GET /api/v1/accounts` | ADMIN/OPERATOR/AUDITOR | 账户数组 | Access 转发 Ledger |
| `GET /api/v1/accounts/{accountNo}` | ADMIN/OPERATOR/AUDITOR | 账户对象 | Access 转发 Ledger |
| `POST /api/v1/accounts` | ADMIN/OPERATOR，必须 CSRF | 201 + 账户对象 | Access 转发 Ledger |
| `GET /api/v1/transfers` | ADMIN/OPERATOR/AUDITOR | 转账数组 | Access 转发 Ledger |
| `GET /api/v1/transfers/{requestId}` | ADMIN/OPERATOR/AUDITOR | 转账对象 | Access 转发 Ledger |
| `GET /api/v1/transfers/{requestId}/ledger` | ADMIN/OPERATOR/AUDITOR | 订单及双边流水证据 | Access 转发 Ledger |
| `POST /api/v1/transfers` | ADMIN/OPERATOR，必须 CSRF | 201 + 转账对象；原样传递 `Idempotency-Key` | Access 转发 Ledger |
| `GET /api/v1/operations/summary` | ADMIN/OPERATOR/AUDITOR | 运行总览 | Access 转发 Operations |

所有未登录与越权响应分别保持 401/403；业务错误保持 `ProblemDetail` 扩展字段 `code`、`detail`、`timestamp`、`traceId`，字段校验错误另含 `errors`。Access 不可把下游堆栈或内部凭据返回给浏览器。

## 内部接口与信任边界

- Ledger 的内部路径以 `/internal/v1/ledger/` 开头，Operations 以 `/internal/v1/operations/` 开头；内部路径不通过 Ingress 暴露。
- Access 使用固定配置中的服务地址，不能根据用户请求中的 URL/Host 参数选择目标。每次调用带服务凭据和追踪号，并有连接/读取超时。
- Ledger/Operations 验证服务凭据之后才处理请求。用户身份由 Access 完成认证和授权；内部服务只接受 Access 生成的受控身份上下文，不信任原始浏览器 `X-User-*` 头。
- Ledger 提供资金数据与统计的只读端点给 Operations；Operations 不读取 Ledger 的 MySQL 表。
- 下游超时或不可用时，Access 返回可关联请求 ID 的 503，而不是伪造空账户、成功交易或健康看板。

## 数据契约

Ledger 独占 `account`、`transfer_order`、`ledger_entry`，保留一次转账的本地事务和唯一幂等键。Access 独占 `app_user`、`app_user_role`、`audit_event`。Operations 独占对账与演练记录。原数据库的 Flyway V1/V2 已执行，不可改写；新服务库迁移和原数据搬迁需通过单独脚本、备份和核对结果验收。

Redis Session 只由 Access 使用；Operations 的聚合缓存可丢弃并重建。三服务各自拥有 `application` 指标标签和稳定请求追踪号，任何指标标签都不使用用户名、账户号或幂等键等高基数字段。
