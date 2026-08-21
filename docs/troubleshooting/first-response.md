# 第一响应排障手册

## 原则

1. 先记录时间、版本、现象和影响范围。
2. 先保留现场，再决定是否重启。
3. 从请求路径外层向内层排查。
4. 每个结论必须对应命令、日志或指标证据。

## 五分钟检查

```bash
date --iso-8601=seconds
uptime
free -h
df -hT
df -ih
ss -lntp
systemctl status opspilot --no-pager
journalctl -u opspilot -n 100 --no-pager
curl -v http://127.0.0.1:18080/actuator/health/readiness
curl -v http://127.0.0.1/health
```

也可以执行：

```bash
bash scripts/inspection.sh
bash scripts/collect-diagnostics.sh
```

## 分层判断

| 现象 | 优先检查 | 常见根因 |
| --- | --- | --- |
| 无法SSH | IP、路由、安全组、sshd | 地址错误、路由、防火墙、服务停止 |
| 80端口拒绝 | `ss`、firewalld、Nginx | 未监听、端口未开放 |
| Nginx 502 | Java端口、应用日志 | 应用未启动、上游地址错误 |
| Nginx 504 | 应用耗时、数据库 | 慢SQL、连接池耗尽、线程阻塞 |
| 应用启动失败 | `journalctl`、环境文件 | 密码错误、端口占用、Flyway失败 |
| 容器反复重启 | `docker inspect/logs` | 进程退出、健康检查、资源限制 |
| 磁盘告警 | `df`、`du`、inode | 日志、镜像层、数据库增长 |
| CPU高 | `top`、`pidstat`、线程栈 | 忙循环、GC、流量突增 |
| 内存高 | `free`、容器限制、JVM | 堆泄漏、缓存、限制过小 |

## 复盘格式

现象 → 告警/日志 → 初始假设 → 执行命令 → 排除过程 → 根因 → 修复 → 验证 → 预防措施。
