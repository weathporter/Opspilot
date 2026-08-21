# OpsPilot 阶段验收报告（2026-08-11）

## 验收结论

当前版本达到“可写入校招简历的个人项目”标准：项目能在 Docker Compose 与 Kubernetes 两条路径运行，业务数据真实落入 MySQL，前端可完成核心操作，发布与回滚有脚本和实机证据，故障边界也有明确说明。

这里的“简历级”不等于“已经在真实银行生产环境运行”。它表示项目同时满足：可运行、可演示、可复现、可测试、可排障、可解释，并且不会把单机 Minikube 夸大为多节点生产集群。

## 实际验证证据

| 验收项 | 结果 | 证据摘要 |
| --- | --- | --- |
| 后端真实数据库测试 | 通过 | 7 项 JUnit/MockMvc/Testcontainers MySQL 测试，0 failure、0 error |
| 前端生产构建 | 通过 | TypeScript 编译与 Vite production build 完成 |
| Docker Compose | 通过 | Web、API、MySQL 与 7 个观测组件共 10 个容器启动；入口、API、MySQL 健康 |
| Edge 业务闭环 | 通过 | 页面创建双账户、转账、原幂等键重放；最终 1 张订单、2 条流水、balanced=true |
| 响应式界面 | 通过 | Microsoft Edge 1440×1000 与 390×844；无横向溢出、无 console/page/HTTP 错误 |
| Helm 静态校验 | 通过 | `helm lint` 为 0 failed；API Server dry-run 可渲染全部资源 |
| Kubernetes 工作负载 | 通过 | API 2/2、Web 2/2、MySQL 1/1，最终 Pod 均 Ready 且 0 restart |
| 数据库启动门禁 | 通过 | API initContainer `wait-for-database` Completed，消除首次并行启动的无效重启 |
| 集群内 Release Test | 通过 | Helm test 从 Web Service 穿过 Nginx 到 API readiness/MySQL，Phase=Succeeded |
| Kubernetes 业务冒烟 | 通过 | 创建账户、250 元转账、幂等重放、双录流水和平衡校验全部通过 |
| 发布与回滚 | 通过 | Helm revision 4 回滚到成功 revision 2，再升级恢复当前 revision 6 |
| 最小权限 | 通过 | operator 可读 Pod；不可读 Secret；不可删除 Pod |
| 存储与保护 | 通过 | 5Gi PVC Bound；API/Web PDB 可用；4 条 NetworkPolicy 已创建 |

## 本机环境限制

- Docker Desktop 的第三方镜像源无法稳定取得 Minikube Ingress Controller 与 Metrics Server 镜像。
- 为保护已有集群，没有删除残留的集群级 Ingress 校验 Webhook；本次实机 Release 使用 `ingress.enabled=false`，通过 Service 端口转发完成验收。
- Chart 中 Ingress 模板已通过 Helm lint 和 Kubernetes 服务端预演，但当前本机没有可工作的 Ingress Controller，因此不能声称“本机 Ingress 已实机通”。
- HPA 对象、2～4 副本和 CPU 70% 策略已经创建；因为 Metrics Server 未运行，当前 `TARGETS` 是 `unknown`。这不影响双副本，但不能声称“本机已经触发自动扩容”。

这些属于运行环境依赖边界，不应通过删除集群安全控制或伪造截图来掩盖。

## 可重复执行

Docker Compose 完整验证：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
cd frontend
npm run test:e2e:edge
```

正常镜像网络下的 Kubernetes 验证：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1
kubectl -n opspilot port-forward service/opspilot-web 18000:8080
powershell -ExecutionPolicy Bypass -File scripts/k8s/smoke-test.ps1 -LocalPort 18001
```

镜像网络受限或集群有失效 Ingress Webhook 时：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s/deploy-minikube.ps1 -SkipAddons -DisableIngress
```

## 界面概念稿对照

最终界面保持了概念稿的关键关系：

1. 左侧稳定导航与顶部环境/主操作区；
2. 首屏四段健康状态，绿色只表达已验证正常；
3. 四项真实业务指标，不使用写死统计数；
4. 趋势区占据主要视觉空间，快捷操作与运行证据位于右侧；
5. 最近转账在首屏下方形成业务回读；
6. 深色金融运维配色、细边框、高信息密度与清晰等宽数据保持一致。

实现没有照搬概念图中的虚构延迟、备份体积和失败订单，而是替换为后端实际可提供的 readiness、账户、转账和流水证据。该差异是有意的数据真实性收敛。

## 投递前仍应完成的个人动作

- 自己从零执行一次 Compose 和 Minikube 部署，不看答案解释每个阶段为什么存在。
- 亲手演示一次余额不足、相同幂等键重放、API Pod 删除自愈和 Helm 回滚。
- 能说清 readiness/liveness/startup、HPA/PDB、StatefulSet/PVC、RBAC/NetworkPolicy 的适用边界。
- 根据真实操作经历修改简历措辞；没有亲自演示过的能力不要写成“熟练掌握”。
