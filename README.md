# NorthLedger

NorthLedger 是一个以资金账户、交易和对账为业务场景的运维实践项目。当前仓库同时保留原来的模块化单体（Compose/本地教学路径）和新的三服务架构（三节点 Kubernetes 实验路径）。后者包含独立的 Access、Ledger、Operations 服务与 Web 前端，以及 GitHub Actions → GHCR → 受保护 Helm 发布 → 冒烟 → 回滚的交付链。**仓库中的配置和测试不等于三台虚拟机已经完成部署；集群实测证据需在实际操作后补充。**

这个项目的重点不是展示组件数量，而是回答三个企业项目问题：业务数据是否一致、每次操作是否可追溯、系统出现故障后能否定位和恢复。

## 业务闭环与本地兼容实现

以下业务与安全链路最先在原单体实现；三服务路径保留浏览器 API 并把数据所有权按服务拆分。运行相关结果以实际测试和集群验收为准。

### 业务闭环

- 创建资金账户，查询账号、户名、余额、币种和状态。
- 发起账户间转账，使用客户端请求号保证网络重试不会重复扣款。
- 在一个 MySQL 事务中完成余额校验、固定顺序加锁、余额更新、订单落库和借贷双边流水。
- 查询转账订单和对应流水，核对借方金额与贷方金额是否平衡。
- 运行总览聚合真实账户、真实交易和真实健康状态，不使用前端假数据。

### 身份与治理闭环

- 用户名和 BCrypt 密码哈希认证；仓库不保存可用登录密码。
- Spring Session Redis 把登录态保存在带 TTL 的共享存储中，多副本 API 可以读取同一会话；浏览器只保存随机会话标识。
- `ADMIN`、`OPERATOR`、`AUDITOR` 三类职责按最小权限访问接口。
- 所有修改请求执行 CSRF 校验；未登录返回 401，越权返回 403。
- 登录成功、登录失败、退出、越权和用户创建写入不可变审计事件。
- 前端菜单按角色呈现，但最终授权始终由 Spring Security 在服务端执行。

### 运维闭环

- Docker 多阶段构建、非 root 运行、健康检查、只读文件系统和 Compose 编排。
- Nginx 统一入口、安全响应头、请求追踪号、登录限流和 Actuator 隔离。
- Prometheus、Grafana、Alertmanager、Loki、Alloy、Node Exporter、cAdvisor 和 Redis Exporter 组成指标、日志和告警链路。
- Helm 管理 Deployment、Service、Ingress、探针、HPA、PDB、RBAC、NetworkPolicy，以及 MySQL/Redis PVC 实验环境。
- Linux 原生路径包含 systemd、Nginx、SELinux、firewalld、logrotate、巡检、诊断、发布和自动回滚脚本。
- MySQL 备份恢复、故障注入、首响排查和复盘模板形成恢复证据。
- 运行总览使用 30 秒短 TTL Redis 缓存；账户或转账事务提交后主动失效，Redis 故障时回源 MySQL 并产生受控指标和日志。

## 界面方向

NorthLedger 采用企业资金平台常见的深海军蓝导航、白色业务画布、表格主导布局。它不是“运维工具大屏”，而是一个业务平台；运维能力通过运行页面、真实指标、日志、告警和部署证据体现。

![NorthLedger 业务门户概念图](docs/ui-concepts/northledger-business-portal-concept.png)

## 请求与数据链路

```text
浏览器
  │  SESSION(HttpOnly) + XSRF-TOKEN/Header
  ▼
Nginx :18000
  ├─ React 静态资源
  ├─ /api → Access :18080 ──内部凭据──▶ Ledger :18081
  │                           └──────────▶ Operations :18082 ──只读──▶ Ledger
  └─ /health → readiness
                 │
                 ├─ Access：Spring Security / RBAC / Audit / Redis Session
                 ├─ Ledger：账户、转账、订单、双边流水（单个本地事务）
                 └─ Operations：运行总览、对账执行与历史、Redis 短时缓存

三服务分别使用 northledger_access、northledger_ledger、northledger_operations 逻辑库和独立数据库账号；共享 MySQL 实例不意味着共享业务表。浏览器不能直接访问内部服务。

Spring Boot / Host / Container ──指标──▶ Prometheus ──▶ Grafana / Alertmanager
Application / Container ─────────日志──▶ Alloy ──▶ Loki ──▶ Grafana
```

## 服务边界

| 服务 | 负责什么 | 不负责什么 |
| --- | --- | --- |
| `services/access-service` | 用户、登录、Redis 会话、CSRF、RBAC、安全审计、对外 API | 直接修改资金表 |
| `services/ledger-service` | 账户、幂等转账、订单和双边流水；只读统计/对账候选接口 | 用户与角色 |
| `services/operations-service` | 总览缓存、对账执行与结果历史 | 写入账户、订单或流水 |
| `frontend` | React 页面、同源访问、对账交互 | 代替后端做权限判断 |

资金写入没有跨服务分布式事务：账户、订单、借贷流水仍在 Ledger 的一个 MySQL 事务里。原 `src/` 单体路径保留为可运行的对照和兼容实现；新服务不读取它的数据库表。分库迁移不会自动搬运旧数据，见[三节点发布与迁移手册](docs/runbooks/three-node-microservices-release.md)。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5、Spring MVC、Validation、JPA/Hibernate、Spring Security、Spring Session Redis、Spring Cache |
| 数据 | MySQL 8.4、Redis 7.4、Flyway、悲观锁、唯一约束、`DECIMAL`、双录流水 |
| 测试 | JUnit 5、MockMvc、Spring Security Test、Testcontainers MySQL/Redis |
| 前端 | React 18、TypeScript、Vite、React Router、Lucide、响应式 CSS |
| 入口与交付 | Nginx、Docker、Docker Compose、GitHub Actions |
| Kubernetes | Helm、Deployment、Service、Ingress、StatefulSet/PVC、HPA、PDB、RBAC、NetworkPolicy |
| 可观测性 | Actuator、Micrometer、Prometheus、Grafana、Alertmanager、Loki、Alloy、Redis Exporter |
| Linux 运维 | Rocky Linux、systemd、firewalld、SELinux、logrotate、Bash、PowerShell |

Redis 只解决两个已经落地的问题：多副本共享会话，以及运行总览的重复聚合读；账户余额、交易订单、流水和审计仍以 MySQL 为唯一事实来源。项目没有加入 Kafka、Milvus、Service Mesh 或多集群，因为当前业务没有需要它们解决的真实问题。

## 本地兼容路径（原模块化单体）

要求：Docker Desktop 已启动，宿主机安装 Java 17、Maven 和 Node.js 20 或更高版本（CI 与前端镜像使用 Node 22）。

```powershell
Copy-Item .env.example .env
# 编辑 .env，为数据库、Redis、首个管理员和 Grafana 设置仅供本机使用的密码
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
```

启动脚本会依次运行真实 MySQL/Redis 集成测试、前端生产构建、镜像构建、Compose 启动和 Nginx 健康门禁。首次数据库为空时，使用 `.env` 中的 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD` 登录。

| 入口 | 地址 | 用途 |
| --- | --- | --- |
| NorthLedger | <http://localhost:18000> | 业务平台统一入口 |
| readiness | <http://localhost:18000/health> | 判断实例能否接收请求 |
| Grafana | <http://localhost:13000> | 指标和日志看板 |
| Prometheus | <http://localhost:19090> | 指标查询与 Targets |
| Alertmanager | <http://localhost:19093> | 告警分组、静默和恢复状态 |
| Loki readiness | <http://localhost:13100/ready> | 日志存储就绪检查 |
| Redis Exporter | <http://localhost:19121/metrics> | Redis 运行指标原始出口 |

## 在 IDEA 中运行后端

1. 用 IDEA 打开项目根目录并选择 Java 17。
2. 先让 MySQL 和 Redis 可用；可以只启动 Compose 中的 `mysql`、`redis` 服务。
3. 在运行配置中提供 `DB_PASSWORD`、`REDIS_PASSWORD`，以及首次空库所需的 `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD`。
4. 运行 `com.opspilot.OpsPilotApplication`；后端监听 `18080`。
5. 前端开发模式在 `frontend` 目录执行 `npm run dev`；完整同源认证建议优先使用 Compose 的 `18000` 入口。

密码没有源码默认值，这是为了让“开发方便”不会意外变成“部署后仍使用公开密码”。

## Kubernetes 微服务路径：两节点优先，三节点可扩展

当前主线的 K8s 交付物位于 `services/{access,ledger,operations}-service`、`deploy/k8s/helm/northledger-microservices`、`deploy/k8s/storage` 和 `scripts/k8s/*-microservices.ps1`。考虑到开发机约 15 GiB 物理内存，**实际演示先用 1 个控制平面 + 1 个 worker**：`values-two-node-lab.yaml` 让四个无状态组件各 1 副本，MySQL/Redis 的 Local PV 均位于 `nl-worker1`。默认 values 仍保留 1 控制平面 + 2 worker、四组件各 2 副本的扩展档位。两种档位的单控制平面、单副本数据库和 Local PV 都不是生产高可用。PV、Secret、Ingress Controller、CNI 网络策略能力和可用 GHCR 镜像必须先准备，Helm 不会创建 VM。

当前优先按[两节点微服务部署手册](docs/runbooks/two-node-microservices-release.md)操作；[VMware 创建与复用步骤](docs/runbooks/two-node-vmware-creation.md)由操作者亲自执行。内存条件允许后再看[三节点扩展方案](docs/runbooks/three-node-microservices-release.md)。对外接口和服务凭据契约见[微服务接口契约](docs/architecture/microservice-contracts.md)，集群内监控安装、故障调查与证据记录见[运维故障教学](docs/runbooks/three-node-observability-and-incidents.md)。这套两节点方案的配置与本地检查不等于虚拟机内实际部署已验收。

这条路径与下面的历史 Minikube/单体教学路径并存，不能把一次 Minikube 演练等同于两节点或三节点微服务实测。

### 历史 Minikube / 单体教学路径

要求：Docker Desktop、Minikube、kubectl 和 Helm。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1
kubectl -n opspilot port-forward service/opspilot-web 18000:8080
powershell -ExecutionPolicy Bypass -File scripts/k8s/smoke-test.ps1
```

Helm values 中的数据库、Redis 和引导管理员密码默认为空，部署时必须通过受控 Secret 或命令行临时值提供。教学用 MySQL/Redis StatefulSet 用于练习 PVC、探针和故障排查；生产示例会关闭内置单节点实例并连接外部高可用 MySQL 与 Redis。

详细边界见 [Kubernetes 交付说明](deploy/k8s/README.md) 与 [生产边界](docs/architecture/kubernetes-production-boundary.md)。

## 验证命令

```powershell
mvn -B -ntp test
Set-Location frontend
npm ci --ignore-scripts
npm audit --audit-level=moderate
npm run build
Set-Location ..
docker compose -f compose.yml -f compose.local.yml config
helm lint deploy/k8s/helm/opspilot `
  --set-string secrets.databasePassword=test-database-password `
  --set-string secrets.mysqlRootPassword=test-root-password `
  --set-string secrets.redisPassword=test-redis-password `
  --set-string secrets.bootstrapAdminPassword=test-bootstrap-password
```

新增三个服务的独立测试与四镜像构建/发布校验：

```powershell
mvn -B -ntp -f services/access-service/pom.xml test
mvn -B -ntp -f services/ledger-service/pom.xml test
mvn -B -ntp -f services/operations-service/pom.xml test
helm lint deploy/k8s/helm/northledger-microservices
powershell -ExecutionPolicy Bypass -File scripts/tests/validate-microservices-chart.ps1
```

本地 `test` 的集成测试需要 Docker Desktop；旧单体与新三服务的测试独立执行。GitHub Actions 在 PR 执行旧单体回归、三服务测试、前端构建与四镜像构建；仅 main 全绿时发布同一提交 SHA 的镜像，实际 Helm 部署还取决于受保护环境审核和自托管 runner。

## 推荐阅读顺序

1. [平台架构与企业级边界](docs/architecture/enterprise-platform-upgrade-spec.md)
2. [Redis 缓存、会话与运维学习手册](docs/learning/redis-cache-session-operations.md)
3. [认证、权限和审计 Runbook](docs/runbooks/authentication-and-access.md)
4. [Docker Compose 运行手册](docs/runbooks/docker-compose.md)
5. [Linux 原生部署](docs/runbooks/linux-deployment.md)
6. [MySQL 备份恢复](docs/runbooks/mysql-backup-restore.md)
7. [第一响应排障](docs/troubleshooting/first-response.md)
8. [个人学习与复述路线](docs/learning-notes/northledger-learning-route.md)
9. [依赖安全审计](docs/security/dependency-audit.md)

## 目录结构

```text
src/                           原单体：Compose/Minikube 兼容路径与回归测试
services/access-service/       认证、Redis Session、RBAC、审计、公开 API
services/ledger-service/       资金事实、事务、幂等转账、流水
services/operations-service/   总览、对账运行及历史
frontend/                      React/TypeScript 业务门户
deploy/docker/                 容器入口与 Nginx
deploy/linux/                  systemd、Nginx、环境和 logrotate
deploy/k8s/helm/opspilot/      Kubernetes Helm Chart
deploy/k8s/helm/northledger-microservices/   三服务 Helm Chart
deploy/k8s/storage/            三节点静态 Local PV 声明
observability/                 Prometheus、Grafana、日志与告警配置
scripts/                       构建、发布、回滚、备份、巡检和演练
docs/                          架构、Runbook、学习与故障材料
.github/workflows/             持续集成门禁
```

## 简历可陈述边界

可以基于真实代码和验证结果说明：将单体按认证入口、资金账务和运行对账拆为三个独立服务；保留资金写入本地事务和 Redis 会话/CSRF；使用 Helm 定义双副本服务、有状态存储与网络隔离；用 GitHub Actions 对每个服务测试并发布确定的 SHA 镜像。只有亲自完成三节点发布、故障演练和恢复后，才可在简历中写“在三节点集群部署并验证闭环”。

不应把单机 MySQL/Minikube 描述成生产高可用集群，也不应在未亲自复现前把配置文件等同于熟练掌握。建议按“能解释原理 → 能独立启动 → 能制造并排除故障 → 能脱离文档复述”的顺序完成个人验收。
