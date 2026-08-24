# 依赖安全审计

复核日期：2026-08-24。

## 门禁结果

| 范围 | 命令 | 当前结果 | 发布判断 |
| --- | --- | --- | --- |
| 前端生产依赖 | `npm audit --audit-level=moderate` | 0 vulnerability | 通过中危及以上门禁 |
| 前端编译 | `npm run build` | TypeScript 与 Vite 生产构建通过，1606 个模块转换 | 通过 |
| 后端行为 | `mvn -B -ntp test` | 26 个测试通过，使用 Testcontainers MySQL 8.4 与带密码 Redis 7.4.10 | 通过 |

## React Router 公告处置

旧版 `react-router`/`react-router-dom` 6.30.6 曾被 npm 报告两条中危公告。项目先完成可达性判断，再升级到包含上游修复的 7.18.2；当前 `npm audit --audit-level=moderate` 为 0 vulnerability，生产构建与现有路由行为均通过。

升级不能只看审计数字：7.18.2 要求 Node.js 20 或更高版本，因此 README、GitHub Actions 和前端容器统一使用 Node 20+/22；当前宿主机 Node 18 只用于临时兼容验证并会出现 engine warning，不是项目声明的构建基线。

[React Router 安全公告](https://github.com/remix-run/react-router/security/advisories/GHSA-wrjc-x8rr-h8h6)用于说明此次升级依据；最终是否可发布仍同时取决于锁文件审计、TypeScript 编译、生产构建和页面验证。

## 持续复核规则

- GitHub Actions 对每次 push/PR 执行 `npm audit --audit-level=moderate`，出现 moderate、high 或 critical 直接失败。
- 更新 `package-lock.json` 后必须重新执行依赖审计和生产构建。
- 中低危公告也必须记录“是否可达、当前缓解、复核日期”，不能只看数量。
- 不在安装依赖时执行第三方生命周期脚本；本地脚本和 CI 使用 `npm ci --ignore-scripts`。
- 不提交 `.env`、Cookie、数据库导出、真实密码或个人信息。

## Flyway 与 MySQL 版本边界

Spring Boot 3.5.4 当前管理的 Flyway 在连接 MySQL 8.4 时会提示“数据库版本高于该 Flyway 版本已经验证的最高版本”。本项目的 V1/V2 迁移及 26 个真实 MySQL/Redis 集成测试均已通过，但这只能证明当前迁移集合可运行，不能替代厂商认证。

[Redgate 当前 MySQL 驱动文档](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql)列出的 verified versions 不包含 8.4。已有本地数据卷也不能直接降级数据库镜像，因此本次保持 MySQL 8.4 LTS 和已验证代码组合；正式生产选型时应重新核对 Spring Boot/Flyway/MySQL 支持矩阵，在备份恢复演练和迁移测试通过后再升级或迁移版本。
