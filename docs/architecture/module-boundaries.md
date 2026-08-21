# OpsPilot 模块边界

本文档定义 OpsPilot 当前模块的职责边界，避免后续加入运维能力时把业务代码、部署脚本和基础设施配置混在一起。

## 当前业务模块

| 模块 | 主要职责 | 不应承担的职责 |
| --- | --- | --- |
| `account` | 账户创建、账户查询、余额与账户状态 | 发布、告警、系统命令执行 |
| `transfer` | 转账编排、幂等校验、事务一致性、账务流水 | 主机管理、监控采集 |
| `common` | 统一异常和跨模块通用类型 | 具体业务规则 |
| `config` | Spring Boot 技术配置 | 业务流程 |

当前采用模块化单体：只有一个可执行应用，但代码按照业务能力分区。这样适合学习、部署和测试，也为以后拆分独立服务保留边界。

## 工程边界

| 目录 | 职责 |
| --- | --- |
| `src/` | Java业务应用及自动化测试 |
| `deploy/linux/` | Rocky Linux、systemd、Nginx与日志轮转配置 |
| `deploy/docker/` | 容器环境配置 |
| `observability/` | Prometheus、Grafana、Alertmanager、Loki和Alloy配置 |
| `scripts/` | 构建、发布、回滚、巡检、备份和故障演练脚本 |
| `docs/` | 架构决策、学习笔记、Runbook与故障复盘 |

## 后续模块预留

后续按实际需求增加 `auth`、`audit`、`deployment`、`alert`、`incident` 模块。未开始对应阶段前不创建空业务模块，避免只有目录没有行为的“空架构”。
