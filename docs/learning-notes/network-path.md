# OpsPilot网络学习地图

只学习与项目直接相关的网络知识，并立即用命令验证。

| 概念 | OpsPilot中的位置 | 验证方式 |
| --- | --- | --- |
| IP与网卡 | Windows、Docker Linux VM、Rocky Linux | `ip addr`、`ip route` |
| 端口 | 80、18000、18080、3306、3307 | `ss -lntp`、`docker port` |
| DNS | Compose服务名`app`、`mysql` | `docker exec ... getent hosts mysql` |
| TCP连接 | Nginx到Spring、Spring到MySQL | `ss -ntp`、`tcpdump` |
| HTTP | `/health`、REST API、Actuator | `curl -v` |
| NAT | Docker端口映射 | 对比容器IP和宿主机端口 |
| 反向代理 | Nginx统一入口 | 对比直连18080和访问80/18000 |
| 防火墙 | Rocky只开放80 | `firewall-cmd --list-all` |

排查顺序：先确认名称解析，再确认路由与端口，再确认TCP连接，最后检查HTTP和业务响应。不要把所有网络问题都归结为“防火墙”。
