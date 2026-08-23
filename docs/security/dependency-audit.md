# 依赖安全审计

复核日期：2026-08-23。

## 门禁结果

| 范围 | 命令 | 当前结果 | 发布判断 |
| --- | --- | --- | --- |
| 前端生产依赖 | `npm audit --audit-level=high` | 0 个 high/critical，2 个 moderate | 通过高危门禁 |
| 前端编译 | `npm run build` | TypeScript 与 Vite 生产构建通过 | 通过 |
| 后端行为 | `mvn -B -ntp test` | 18 个测试通过，使用 Testcontainers MySQL 8.4 | 通过 |

## React Router 中危公告判断

当前 `react-router`/`react-router-dom` 6.30.6 被 npm 报告两条中危公告：

1. 开放重定向/反斜杠路径绕过。NorthLedger 唯一使用外部状态回跳的位置在登录页；代码只接受以单个 `/` 开始、不以 `//` 开始且不包含反斜杠的站内路径，其余统一回到 `/`。
2. SSR hydration 错误反序列化。NorthLedger 使用 Vite 客户端单页应用，不运行 React Router 服务端渲染和 `deserializeErrors()` 链路，当前不可达。

`npm audit fix --force` 当前会把路由强制降到 5.3.4，属于破坏性主版本回退，会使现有 v6 路由代码失效。因此不以“清零数字”为目标执行该命令，而是保留可达性判断并在上游提供兼容修复版本后升级。

## 持续复核规则

- GitHub Actions 对每次 push/PR 执行 `npm audit --audit-level=high`，出现 high 或 critical 直接失败。
- 更新 `package-lock.json` 后必须重新执行依赖审计和生产构建。
- 中低危公告也必须记录“是否可达、当前缓解、复核日期”，不能只看数量。
- 不在安装依赖时执行第三方生命周期脚本；本地脚本和 CI 使用 `npm ci --ignore-scripts`。
- 不提交 `.env`、Cookie、数据库导出、真实密码或个人信息。

## Flyway 与 MySQL 版本边界

Spring Boot 3.5.4 当前管理的 Flyway 在连接 MySQL 8.4 时会提示“数据库版本高于该 Flyway 版本已经验证的最高版本”。本项目的 V1/V2 迁移及 18 个真实 MySQL 8.4 测试均已通过，但这只能证明当前迁移集合可运行，不能替代厂商认证。

[Redgate 当前 MySQL 驱动文档](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql)列出的 verified versions 不包含 8.4。已有本地数据卷也不能直接降级数据库镜像，因此本次保持 MySQL 8.4 LTS 和已验证代码组合；正式生产选型时应重新核对 Spring Boot/Flyway/MySQL 支持矩阵，在备份恢复演练和迁移测试通过后再升级或迁移版本。
