# Docker Compose运行Runbook

## Windows + Docker Desktop

基础环境：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

包含监控日志：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability
```

同时执行Testcontainers测试：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
```

`compose.desktop-observability.yml`只解决Docker Desktop不支持`rslave`挂载传播的问题。Rocky Linux不使用这个覆盖文件。

## Rocky Linux

```bash
bash scripts/build.sh
docker compose -f compose.yml -f compose.local.yml up -d --build
docker compose -f compose.yml -f compose.local.yml -f compose.observability.yml up -d
```

在CI环境可以直接使用多阶段Dockerfile：

```bash
docker compose -f compose.yml build
```

## 访问地址

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| Nginx | `http://localhost:18000` | 对外入口 |
| 应用直连 | `http://localhost:18080` | 仅本机调试 |
| Grafana | `http://localhost:13000` | 仪表盘和日志查询 |
| Prometheus | `http://localhost:19090` | 指标与告警规则 |
| Alertmanager | `http://localhost:19093` | 告警分组和静默 |
| Loki | `http://localhost:13100/ready` | 日志存储健康 |
| Alloy | `http://localhost:12345` | 采集器状态 |
| cAdvisor | `http://localhost:18081` | 容器指标 |

## 日常排查

```bash
docker compose -f compose.yml -f compose.local.yml ps
docker logs --tail 100 opspilot-app-1
docker inspect opspilot-app-1
docker network inspect opspilot_network
docker volume inspect opspilot_mysql_data
docker stats --no-stream
```

## 停止但保留数据

```bash
docker compose -f compose.yml -f compose.local.yml -f compose.observability.yml down
```

不要随意增加`-v`。`down -v`会删除命名卷，其中包括MySQL、Prometheus、Grafana和Loki的数据。
