# OpsPilot 全景百科与实战学习手册

这是一套围绕 `D:\Develop\OpsPilot` 编写的项目教材，不是技术名词清单。它把业务、代码、数据库、Linux、网络、Docker、监控告警、Kubernetes、发布恢复和面试表达放进同一条证据链。

## 两种阅读方式

- [打开完整合订本](OpsPilot-全景百科与实战学习手册.md)：适合全文搜索、离线阅读和统一保存。
- 按下表分篇阅读：更接近语雀知识库，适合每学完一篇就做对应实验。

| 分篇 | 范围 | 重点 |
|---|---|---|
| [00-使用说明与项目全貌.md](chapters/00-使用说明与项目全貌.md) | 使用说明、第 1—4 章 | 项目定位、技术栈、架构与目录边界 |
| [01-Linux与网络.md](chapters/01-Linux与网络.md) | 第 5—9 章 | Linux、systemd、Nginx、网络与值班方法 |
| [02-Docker与Compose.md](chapters/02-Docker与Compose.md) | 第 10—13 章 | 镜像、容器、网络、卷、Dockerfile 与 Compose |
| [03-Java与MySQL.md](chapters/03-Java与MySQL.md) | 第 14—23 章 | Spring Boot、REST、事务、幂等、锁与测试 |
| [04-前端与可观测性.md](chapters/04-前端与可观测性.md) | 第 24—32 章 | React、联调、Prometheus、Grafana、Loki 与告警 |
| [05-Kubernetes与Helm.md](chapters/05-Kubernetes与Helm.md) | 第 33—46 章 | 工作负载、网络、存储、探针、弹性、安全与 Helm |
| [06-交付安全与企业演进.md](chapters/06-交付安全与企业演进.md) | 第 47—56 章 | 运行、Linux 部署、发布回滚、备份、安全与路线 |
| [07-学习实训与面试.md](chapters/07-学习实训与面试.md) | 第 57—63 章 | 8 周路线、24 个实训、演示脚本与 67 个追问 |
| [08-故障百科与值班参考.md](chapters/08-故障百科与值班参考.md) | 第 64—74 章 | 分层排障、工具参考、词典与最终验收 |

## 建议学习顺序

1. 先读项目全貌，再亲手跑通 Compose 业务闭环；
2. 按 Linux/网络 → Docker → Java/MySQL 的顺序掌握主链路；
3. 再学习前端联调、监控日志告警和 Kubernetes；
4. 最后完成实训、故障演练、项目演示和面试复述。

## 使用原则

- “代码存在”不等于“本人掌握”，必须亲手运行、观察、制造故障并验收；
- 历史验收只能当检查清单，不能代表当前环境仍在线；
- 单机 Minikube、本地 MySQL 和空 Alertmanager 接收器不能包装成生产高可用；
- AI/Codex 是辅助工具，项目能力归属来自你对代码、运行结果和故障证据的独立负责。

最后更新：2026-08-13
