# NorthLedger 0.3.0 验收报告（2026-08-24）

## 1. 验收结论

NorthLedger 0.3.0 已达到“可写入校招简历的个人项目”交付标准：资金账户、幂等转账、双录流水、身份权限、安全审计、React 门户、MySQL 业务事实、Redis 共享会话与短 TTL 缓存、Compose 可观测性、Linux 原生发布和 Kubernetes Helm 交付形成了可运行、可测试、可排障、可回滚和可解释的闭环。

“简历级”不等于真实银行生产系统。本项目没有承载真实资金；Docker Desktop、单副本 MySQL/Redis 与 Minikube 都是本地实战环境，不描述为生产高可用。生产覆盖文件关闭内置状态组件，连接外部高可用 MySQL/Redis，并为外部 Redis 启用 TLS 与企业私有 CA 信任材料；该生产覆盖已完成 Helm 静态渲染，未伪称已经连接真实企业托管服务。

## 2. 本次新鲜验证证据

| 验收项 | 结果 | 2026-08-24 实际证据 |
| --- | --- | --- |
| 后端全量测试 | 通过 | 26 个 JUnit/MockMvc/Testcontainers 测试，真实 MySQL 8.4 与带密码 Redis 7.4.10；0 failure、0 error、0 skipped |
| Redis 会话 | 通过 | 登录后 Session 写入 `northledger:session:*`；Kubernetes 双 API 副本无需粘性会话；登出主动清理 |
| Redis 总览缓存 | 通过 | 固定命名空间、JSON 序列化、30～40 秒抖动 TTL、不缓存 null；本地运行观测 TTL=32 秒 |
| 一致性与降级 | 通过 | 账户/转账在数据库提交后触发缓存失效；真实 Redis 中断时总览回源 MySQL 并记录低基数指标，Session/readiness 保持关键依赖边界 |
| 身份与权限 | 通过 | 匿名 401、角色 403、CSRF、管理员用户管理、失败登录审计和服务端会话均有集成测试 |
| 前端生产构建 | 通过 | TypeScript 与 Vite build 成功，1606 个模块转换；产物约 248 kB JS、28 kB CSS（压缩前） |
| 前端依赖门禁 | 通过 | `npm audit --audit-level=moderate` 为 0 vulnerability；React Router 固定到已修复安全公告的 7.18.2 |
| Compose 完整环境 | 通过 | MySQL、Redis、API、Web、Prometheus、Grafana、Alertmanager、Loki、Alloy、Node Exporter、cAdvisor、Redis Exporter 共 12 个服务运行 |
| 本地业务闭环 | 通过 | 匿名会话、管理员登录、重复总览缓存复用、管理员角色与退出均实际验证 |
| Redis 运行策略 | 通过 | 认证 PING、AOF、`noeviction`、回环端口 16379、Exporter `redis_up=1` |
| 可观测性 | 通过 | Prometheus 6/6 Targets up、8 条规则加载；Grafana 自动发现 1 个 NorthLedger Redis 看板；Alertmanager/Loki readiness 返回 200 |
| 故障演练 | 通过 | 长时演练曾观察 `RedisUnavailable` 从 pending 到 firing 并进入 Alertmanager，恢复后归零；最终 20 秒有界演练再次验证自动恢复和入口 `UP` |
| Helm 本地模式 | 通过 | 内置 MySQL/Redis、PVC、Exporter、NetworkPolicy、Secret、探针、HPA/PDB/RBAC 均可 lint/render |
| Helm 外部生产模式 | 静态通过 | 关闭内置 MySQL/Redis，渲染 `rediss://`、Spring SSL bundle、PKCS12 truststore、Exporter PEM CA 与 monitoring 命名空间网络放行 |
| Linux 交付契约 | 通过 | API/Web 原子发布、Nginx、Redis 专用最小密钥文件、巡检与诊断契约通过；EnvironmentFile 空格/元字符按普通数据解析 |

## 3. Kubernetes 真实集群证据

本次不是只做 `helm template`。在 Docker Desktop 驱动的 Minikube 中完成 NorthLedger 0.3.0 revision 13 实际升级：

- Helm Release 状态为 `deployed`，Helm Release Test 为 `Succeeded`。
- API 2/2、Web 2/2、MySQL 1/1、Redis 1/1、Redis Exporter 1/1 全部 Ready。
- MySQL PVC 5 GiB、Redis PVC 512 MiB 均为 Bound；已有 MySQL PVC 升级时复用了首次初始化凭据，没有删除或重建数据卷。
- 默认拒绝入站后，分别放行 Web→API、API→MySQL/Redis、Exporter→Redis、Prometheus→API/Exporter。
- Docker Desktop 构建与运行 Pod 内 `app.jar` 的 SHA-256 一致，排除了同标签节点缓存继续运行旧镜像的问题。
- 8 步集群冒烟真实完成管理员登录、创建账户、转账、相同幂等键重放、两条双录流水、`balanced=true`、Redis Session、有限 TTL 缓存、数据库提交后缓存键删除与再次生成。
- 本次冒烟 requestId 为 `k8s-smoke-0824112840`，重新生成缓存 TTL=30 秒，`cacheEvictedAfterCommit=True`。

上述真实集群记录产生于 revision 13。其后的最终审查又补上了“既有 Redis PVC 复用匹配密码”、Chart-managed Secret 校验和、Helm 3/4 回滚参数兼容和内置/外部 Redis 凭据轮换分流。这些后续修复已通过 PowerShell 5.1/7 解析、Bash 语法、Helm 双模式 lint/render 与 Redis 交付契约，但没有再次启动 Minikube 生成新 revision；因此不把静态复验写成第二次集群实测。

本机受 7.5 GiB Docker 内存限制，Minikube 验收后执行的是 `minikube stop` 而不是删除集群；PVC 与 Helm history 保留。目前恢复运行完整 Compose 演示环境。因为最终部署使用 `-SkipAddons -DisableIngress`，HPA 对象存在但 Metrics Server 未启用，`TARGETS` 显示 unknown，不能据此声称已经触发真实自动扩容。

## 4. 可重复执行门禁

后端与前端：

```powershell
mvn -B -ntp test
Set-Location frontend
npm ci
npm audit --audit-level=moderate
npm run build
```

Compose、Linux 与 Redis 交付：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/tests/validate-linux-release.ps1
powershell -ExecutionPolicy Bypass -File scripts/tests/validate-redis-delivery.ps1
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability
```

Helm 内置依赖模式：

```powershell
helm lint deploy/k8s/helm/opspilot `
  --set-string secrets.databasePassword=test-database-password `
  --set-string secrets.mysqlRootPassword=test-root-password `
  --set-string secrets.redisPassword=test-redis-password `
  --set-string secrets.bootstrapAdminPassword=test-bootstrap-password
```

Helm 外部 TLS 模式与真实 Minikube：

```powershell
helm lint deploy/k8s/helm/opspilot -f deploy/k8s/helm/opspilot/values-production.example.yaml
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1 -SkipAddons -DisableIngress
powershell -ExecutionPolicy Bypass -File scripts/k8s/smoke-test.ps1
```

## 5. 当前边界与个人验收

- MySQL 仍是账户、转账、流水、用户和审计的唯一事实来源；Redis 只保存可过期会话和可重建总览快照。
- 内置 Redis 是单副本学习环境。AOF 与 PVC 能练习普通重建恢复，但不能替代跨节点故障转移、备份恢复演练和容量治理。
- 外部 TLS values 是可渲染交付接口，不等于已经获得企业 Redis、证书、SLA 或备份权限。
- GitHub Actions 工作流已加入后端/前端、Linux、Redis、Helm 双模式和 Prometheus 门禁；是否远端绿色必须以本次推送后的 GitHub 运行结果为准。
- 投递前本人应能从零启动、解释 MySQL/Redis 数据边界、制造并恢复 Redis 故障、读懂 PromQL/告警状态、完成一次 Helm 部署与回滚，并按真实掌握程度使用“了解、熟悉、熟练”。
