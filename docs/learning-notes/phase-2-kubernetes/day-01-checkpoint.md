# Day 1 理解检查与实验记录

姓名和日期不需要写入 Git。本文件用于记录技术观察，不记录密码、令牌和隐私数据。

## A. 先闭卷回答

### 1. 镜像和容器有什么区别？

我的回答：

### 2. 为什么 `docker ps` 和 `docker ps -a` 的结果不同？

我的回答：

### 3. `18000:80` 左右两边分别代表什么？

我的回答：

### 4. 应用为什么连接 `mysql:3306`，而不是 `localhost:3307`？

我的回答：

### 5. 为什么不能把 `172.19.0.2` 写死在应用配置中？

我的回答：

### 6. MySQL 容器重建后，数据为什么可以保留？

我的回答：

### 7. 容器 Running、容器 healthy、业务正确有什么区别？

我的回答：

### 8. 一次开户请求经过哪些主要 Java 类？

我的回答：

### 9. `@Valid` 和 `@Transactional` 分别在哪一层发挥作用？

我的回答：

### 10. 为什么退出码 137 不能直接证明 Java 发生 OOM？

我的回答：

## B. 观察实验

### 实验 1：查看容器生命周期

```powershell
docker ps
docker ps -a
```

观察到的差异：

### 实验 2：查看三容器网络

```powershell
docker network inspect opspilot_network
```

记录服务名，不要求记忆临时 IP：

```text
nginx：
app：
mysql：
```

### 实验 3：检查外部入口和应用直连

```powershell
Invoke-RestMethod http://localhost:18000/health
Invoke-RestMethod http://localhost:18080/actuator/health/readiness
```

两个请求分别验证了哪些链路：

### 实验 4：查看应用身份

```powershell
docker exec opspilot-app-1 id
```

为什么使用非 root 用户：

### 实验 5：查看启动日志

```powershell
docker logs --tail 100 opspilot-app-1
```

在日志中找到：

- Hikari 数据库连接池启动：
- Flyway 校验：
- 当前迁移版本：
- Tomcat 端口：
- 应用启动耗时：

## C. 画出请求路径

请补完整：

```text
客户端
→ Windows 端口 ______
→ ______ 容器端口 ______
→ Docker DNS 名 ______:______
→ Spring 类 ______________________________
→ Spring 类 ______________________________
→ Repository _____________________________
→ Docker DNS 名 ______:______
→ MySQL 表 ______
```

## D. Java 源码定位

请在代码中找到并写出文件路径：

| 问题 | 文件路径/方法 |
|---|---|
| POST 路由在哪里定义 | |
| 参数校验在哪里定义 | |
| 事务边界在哪里 | |
| 创建 Account 对象在哪里 | |
| INSERT 尽快发送给数据库在哪里 | |
| 重复账号异常在哪里转换 | |
| JPA Repository 在哪里 | |
| 实体转换成响应 DTO 在哪里 | |

## E. 最小故障判断题

### 场景 1

`docker ps` 显示 app 为 Running，但访问 `/health` 返回 502。

我会先检查：

### 场景 2

`docker ps -a` 显示 MySQL 为 `Exited (137)`。

在下结论前，我还需要哪些证据：

### 场景 3

删除并重建 app 容器后 IP 改变，但 Nginx 应继续通过什么名称访问它：

我的回答：

## F. 自我评分

| 能力 | 0 不会 | 1 能跟做 | 2 能解释 | 3 能独立排查 |
|---|---:|---:|---:|---:|
| 镜像与容器 | | | | |
| 端口映射 | | | | |
| Docker 网络/DNS | | | | |
| 数据卷 | | | | |
| Compose 启动顺序 | | | | |
| Java 请求链 | | | | |
| 日志与健康检查 | | | | |

总分不作为最终目标。任何一项低于 2，都应该重新完成对应实验后再进入 Day 2。
