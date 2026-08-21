# 第一阶段学习路线

学习方式固定为：业务原因 → 底层机制 → 手动操作 → 自动化 → 故障排查 → 面试表达。不要先背命令，再猜命令为什么存在。

## 第0课：先认识系统边界

目标：能画出请求路径，知道每一层负责什么。

```text
客户端
  → Linux网卡与路由
  → firewalld 80端口
  → Nginx
  → 127.0.0.1:18080 Spring Boot
  → HikariCP连接池
  → MySQL 3306
```

你的任务：不看本文，重新画一遍，并给每条连接标出源地址、目标地址和端口。

## 第1课：Linux原生运行

学习内容：用户和权限、进程、端口、systemd、journal、文件系统、firewalld、SELinux、Nginx。

必须亲手执行：

```bash
ps -ef | grep java
ss -lntp
systemctl status opspilot
journalctl -u opspilot -n 100 --no-pager
curl -v http://127.0.0.1:18080/actuator/health/readiness
curl -v http://127.0.0.1/health
```

要理解：进程存在不代表服务可用；端口监听不代表依赖正常；HTTP 200也不代表所有业务正确。

## 第2课：Shell自动化

先手动发布一次，再阅读`deploy.sh`。重点观察：

- `set -Eeuo pipefail`如何让失败尽早暴露。
- `flock`如何避免两个发布同时执行。
- 版本目录和`current`软链接如何实现切换。
- 健康检查失败后如何恢复上一链接。
- 为什么失败版本仍被保留用于取证。

你的任务：给`health-check.sh`增加一个合法参数，并为非法参数验证退出码。

## 第3课：MySQL备份恢复

学习内容：逻辑备份、`--single-transaction`、校验和、恢复前备份、最小权限。

你的任务：备份开发库，使用`gzip -t`和`sha256sum -c`校验，再恢复到单独的测试库。不要第一次就覆盖原库。

## 第4课：Docker与Compose

使用以下命令观察，而不只运行`up`：

```bash
docker image history opspilot-app
docker inspect opspilot-app-1
docker network inspect opspilot_network
docker volume inspect opspilot_mysql_data
docker stats --no-stream
docker logs --tail 100 opspilot-app-1
```

必须能解释：镜像层、容器可写层、命名卷、端口映射、服务名DNS、健康检查、非root用户、只读根文件系统。

## 第5课：监控、日志和告警

在Prometheus中练习：

```promql
up{job="opspilot"}
rate(http_server_requests_seconds_count{application="opspilot"}[1m])
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="opspilot"}[5m])))
sum(jvm_memory_used_bytes{application="opspilot",area="heap"})
```

在Grafana Explore中查询：

```logql
{service="app"}
{service="app"} |= "ERROR"
```

你的任务：调用一次转账接口，记录响应中的`X-Request-ID`，再在日志中找到同一个ID。

## 第6课：故障演练与复盘

只在实验机运行：

```bash
bash scripts/inject-failure.sh --scenario stop-app --duration 30 --confirm LAB_ONLY
bash scripts/collect-diagnostics.sh
```

按“现象 → 证据 → 假设 → 排除 → 根因 → 修复 → 预防”写复盘。禁止看到告警后直接重启而不保留现场。

## 面试表达

30秒版本：

> 我把一个带事务和幂等的Spring Boot转账服务部署到Linux，使用systemd和Nginx管理入口，Shell实现版本化发布、健康检查和失败回滚，再用Docker Compose复现MySQL与应用环境，并接入Prometheus、Grafana、Loki和Alertmanager。项目包含受控故障注入、诊断信息收集和MySQL备份恢复。

2分钟版本需要增加一个真实排障案例，并解释为什么做出当前架构选择。
