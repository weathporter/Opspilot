# 第一阶段验收清单

第一阶段分为“工程实现”和“个人实操”两部分。代码被创建不等于已经掌握，只有你在Rocky Linux上独立完成一次并能解释排障路径，学习阶段才算完成。

## 工程实现状态

| 验收项 | 状态 | 证据 |
| --- | --- | --- |
| 模块边界与企业演进路线 | 已完成 | `docs/architecture/`、`docs/decisions/` |
| Spring Boot构建 | 已验证 | Java 17、Maven构建成功 |
| MySQL真实集成测试 | 已验证 | 6个测试通过，0失败 |
| 请求ID与错误关联 | 已验证 | `CorrelationIdFilter`及单元测试 |
| systemd、Nginx、环境变量、logrotate | 已实现 | `deploy/linux/` |
| 构建、发布、健康检查、回滚 | 已实现 | `scripts/` |
| MySQL备份与安全恢复 | 已实现 | 备份校验、恢复确认、恢复前备份 |
| Docker非root运行 | 已验证 | 运行时镜像和Compose应用健康 |
| Nginx统一入口 | 已验证 | `http://localhost:18000/health`返回200 |
| Prometheus抓取 | 已验证 | app、node、cadvisor、loki、prometheus目标全部up |
| Grafana自动配置 | 已验证 | Prometheus、Loki数据源和总览仪表盘已加载 |
| Loki日志采集 | 已验证 | Alloy已采集全部Compose服务日志 |
| Alertmanager与告警规则 | 已验证 | 两个规则组已加载，Alertmanager就绪 |
| 故障注入与诊断包 | 已实现 | 有范围限制、时间限制和显式确认 |

## 需要你亲自完成

- [ ] 创建或准备一台Rocky Linux 9实验机。
- [ ] 独立解释客户端到MySQL的请求路径。
- [ ] 手动部署一次JAR，不先使用自动部署脚本。
- [ ] 再使用脚本完成一次新版本发布。
- [ ] 制造一次错误配置并按Runbook定位。
- [ ] 完成一次MySQL备份、校验和测试库恢复。
- [ ] 在Grafana中找到一次请求对应的指标与日志。
- [ ] 使用复盘模板写一份故障报告。
- [ ] 用30秒和2分钟两种长度讲解第一阶段。

## 第一阶段完成定义

你不看文档也能回答以下问题时，第一阶段才真正结束：

1. 为什么应用只监听`127.0.0.1:18080`，而Nginx监听80？
2. systemd如何判断进程退出，并在什么情况下重启？
3. Docker端口映射和容器网络中的服务名解析有什么区别？
4. readiness失败时为什么不一定要重启服务器？
5. 如何区分CPU、内存、磁盘、网络、Java和MySQL问题？
6. 发布失败后，脚本怎样避免把不健康版本继续暴露？
