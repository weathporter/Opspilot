# 企业级演进路线

OpsPilot采用“当前可完成、后续可扩展”的路线。企业级空间通过稳定的配置、接口和部署边界预留，不通过提前安装大量中间件实现。

## 阶段一：单机可独立运行

- Rocky Linux 9原生部署
- systemd进程托管
- Nginx统一入口
- Shell构建、发布、健康检查和回滚
- MySQL备份恢复
- Docker Compose复现环境
- Prometheus、Grafana、Alertmanager、Loki和Alloy
- Linux、Java、MySQL、Docker常见故障Runbook

完成标准：能够从一台空白Linux主机部署应用，能够发现故障、定位原因、恢复服务并留下记录。

## 阶段二：企业化交付

- dev、test、prod环境隔离
- Ansible主机初始化和批量发布
- Jenkins流水线、镜像扫描和制品管理
- Kubernetes、Helm、滚动更新、探针、资源限制和自动回滚
- RBAC、操作审计、发布记录与告警事件

完成标准：相同制品能够经过自动化流程发布到不同环境，失败时阻断或回滚。

## 阶段三：可靠性与高级可观测性

- OpenTelemetry和Tempo链路追踪
- SLI、SLO和错误预算
- 指标、日志、链路和发布事件关联
- Redis、RabbitMQ与Outbox按真实场景引入
- 灰度发布、容量测试与更复杂的故障演练

完成标准：能够用统一请求ID或Trace ID解释一次慢请求，并通过SLO衡量系统可靠性。

## 明确暂缓

Milvus、Istio、多集群、复杂Operator和大规模高可用中间件不进入第一阶段。以后只有出现可验证的业务需求时再加入。
