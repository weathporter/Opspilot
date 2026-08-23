# NorthLedger 阶段验收报告（2026-08-23）

## 验收结论

当前版本达到“可写入校招简历的个人项目”标准：资金账户、幂等转账、双录流水、身份权限、安全审计、前端门户、容器部署和可观测性形成了可运行闭环，构建和部署路径有自动化门禁，生产边界有明确说明。

“简历级”表示可运行、可复现、可测试、可排障和可解释，不表示项目已经承载真实银行资金，也不把单机 MySQL、Docker Desktop 或 Minikube 描述成生产高可用环境。

## 本次实时验证证据

| 验收项 | 结果 | 证据摘要 |
| --- | --- | --- |
| 后端全量测试 | 通过 | 18 个 JUnit/MockMvc/Testcontainers MySQL 测试，0 failure、0 error |
| 身份与权限契约 | 通过 | 匿名 401、角色 403、CSRF、服务端会话、管理员用户管理均有集成测试 |
| 安全审计与指标 | 通过 | 失败登录产生审计事件和低基数 Prometheus 指标，响应不含密码 |
| 前端生产构建 | 通过 | TypeScript 编译与 Vite production build 完成，1598 个模块转换成功 |
| 前端依赖门禁 | 通过 | 0 个 high/critical；2 个 moderate 已完成可达性判断与缓解记录 |
| Compose 配置 | 通过 | 基础与完整观测覆盖文件可解析 |
| Helm 静态校验 | 通过 | Chart lint 为 0 failed，Secret 值通过一次性测试参数注入 |
| Docker 镜像 | 通过 | API 与 Web runtime 镜像构建成功，均以非 root 用户运行 |
| Compose 完整环境 | 通过 | MySQL、API、Web、Prometheus、Grafana、Alertmanager、Loki、Alloy、Node Exporter、cAdvisor 启动 |
| 真实会话闭环 | 通过 | 管理员登录、读取运行汇总/账户/用户/审计并退出成功 |
| 数据连续性 | 通过 | 原有 MySQL volume 原地升级，验收时读取到 13 个既有账户 |
| 入口健康 | 通过 | NorthLedger、Grafana、Prometheus、Alertmanager、Loki readiness 均返回 2xx |
| 安全响应头 | 通过 | 实际入口返回 CSP、frame、nosniff、referrer 与 permissions policy，且首页保持 no-cache |

## Kubernetes 证据边界

Helm Chart 当前包含双副本 API/Web、三类探针、资源请求与限制、HPA、PDB、RBAC、NetworkPolicy、Ingress 和教学用 MySQL StatefulSet/PVC，并已通过本次 Helm lint。

2026-08-11 的既有实操记录曾验证 Minikube 中的 Pod Ready、Helm test、业务冒烟、版本回滚和最小权限；当前 Minikube 已停止，本次没有把历史状态当作实时状态重新声明。再次投递前可按 `deploy/k8s/README.md` 从零复现并更新实操记录。

内置单实例 MySQL 只用于学习 StatefulSet、PVC、探针、备份和恢复。生产 profile 关闭内置数据库并要求外部高可用 MySQL；HPA 只有在 Metrics Server 可用时才会根据指标执行扩缩容。

## 可重复执行

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
```

```powershell
mvn -B -ntp test
Set-Location frontend
npm audit --audit-level=high
npm run build
```

```powershell
helm lint deploy/k8s/helm/opspilot `
  --set-string secrets.databasePassword=test-database-password `
  --set-string secrets.mysqlRootPassword=test-root-password `
  --set-string secrets.bootstrapAdminPassword=test-bootstrap-password
```

## 投递前个人验收

- 从空白环境独立完成一次 Compose 启动，不依赖自动补救。
- 用管理员、操作员和审计员分别验证权限矩阵，并解释 401、403 与 CSRF 失败。
- 演示余额不足、幂等重放、错误密码、越权和 MySQL 暂停等故障。
- 用 traceId 串联页面错误、审计记录、应用日志和 Prometheus 趋势。
- 从零复现一次 Helm 部署、失败发布和回滚，区分配置存在与亲自验证。
- 根据真实掌握程度使用“了解、熟悉、熟练”，不能把仓库中存在的文件直接等同于个人能力。
