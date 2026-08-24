# Rocky Linux 原生部署 Runbook

目标：Spring Boot 由 systemd 管理并只监听本机 18080，Nginx 在 80 端口同时提供 React 门户和 `/api` 反向代理。JAR 与前端静态文件进入同一个不可变版本目录，发布和回滚始终保持前后端一致。建议先在虚拟机或云实验机操作，不要直接用于真实生产主机。

## 1. 开发机完成构建

```bash
bash scripts/build.sh
sha256sum target/northledger-0.3.0.jar
sha256sum frontend/dist/index.html
```

企业环境通常在构建节点生成制品，生产主机只运行JRE，不安装Maven和完整源代码。

## 2. 复制引导文件与制品

```bash
scp -r deploy scripts linux-user@SERVER_IP:/tmp/opspilot-bootstrap/
scp target/northledger-0.3.0.jar linux-user@SERVER_IP:/tmp/northledger.jar
scp -r frontend/dist linux-user@SERVER_IP:/tmp/northledger-web
```

## 3. 初始化Rocky Linux

```bash
ssh linux-user@SERVER_IP
cd /tmp/opspilot-bootstrap
sudo bash scripts/init-server.sh
```

脚本完成以下操作：安装JRE、Nginx、MySQL/Redis客户端和排障工具，创建`opspilot`服务用户，创建受控目录，安装systemd/Nginx/logrotate配置，处理firewalld和SELinux所需规则。`redis` 包在这里只提供 `redis-cli` 诊断能力；脚本不启用主机 `redis.service`，实际 Redis 仍由后文容器管理。

## 4. 准备MySQL

学习阶段可以先使用单独的MySQL 8.4容器，Java应用仍按原生方式运行：

```bash
sudo bash scripts/install-docker.sh --user "$USER"
sudo docker volume create opspilot_mysql_data
sudo docker run -d \
  --name opspilot-mysql \
  --restart unless-stopped \
  -e MYSQL_DATABASE=opspilot \
  -e MYSQL_USER=opspilot \
  -e MYSQL_PASSWORD='CHANGE_ME' \
  -e MYSQL_ROOT_PASSWORD='CHANGE_ROOT_ME' \
  -p 127.0.0.1:3306:3306 \
  -v opspilot_mysql_data:/var/lib/mysql \
  mysql:8.4
```

生产架构应使用独立高可用数据库，不能把上述单容器描述成生产高可用方案。

## 5. 配置应用与依赖凭据

```bash
sudo vi /etc/opspilot/opspilot.env
sudo vi /etc/opspilot/redis.env
sudo chown root:opspilot /etc/opspilot/opspilot.env
sudo chmod 0640 /etc/opspilot/opspilot.env
sudo chown root:root /etc/opspilot/redis.env
sudo chmod 0600 /etc/opspilot/redis.env
```

至少替换应用文件中的 `DB_PASSWORD=CHANGE_ME` 与 `REDIS_PASSWORD=CHANGE_ME`，并把同一 Redis 密码写入专用 `redis.env`。为全新数据库填写 `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD`，确认 `DB_URL`、`REDIS_HOST` 和 `REDIS_PORT` 与依赖位置一致。不要把真实密码放入仓库或命令历史。

## 6. 准备Redis

学习环境可以让 systemd 管理的 Java 连接同机 Redis 容器；端口只绑定回环地址。Redis 只读取 root 专用的最小环境文件，不会继承数据库密码、管理员密码和 JVM 配置：

```bash
sudo docker volume create opspilot_redis_data
sudo docker run -d \
  --name opspilot-redis \
  --restart unless-stopped \
  --env-file /etc/opspilot/redis.env \
  -p 127.0.0.1:6379:6379 \
  -v opspilot_redis_data:/data \
  -v /tmp/opspilot-bootstrap/deploy/redis/entrypoint.sh:/opt/northledger/redis-entrypoint.sh:ro \
  --read-only \
  --tmpfs /run/redis:size=1m,mode=0700 \
  --security-opt no-new-privileges:true \
  --entrypoint /bin/sh \
  redis:7.4.10-alpine /opt/northledger/redis-entrypoint.sh
```

该单容器用于学习 AOF、TTL、认证和故障演练，不是生产高可用 Redis。生产应连接企业托管的高可用 Redis，并在不可信网络上启用 TLS。

## 7. 首次发布

```bash
cd /tmp/opspilot-bootstrap
sudo bash scripts/deploy.sh \
  --artifact /tmp/northledger.jar \
  --web-dist /tmp/northledger-web \
  --version 0.3.0
```

## 8. 验证

```bash
systemctl status opspilot --no-pager
journalctl -u opspilot -n 100 --no-pager
ss -lntp | grep -E ':80|:18080|:3306|:6379'
curl -v http://127.0.0.1:18080/actuator/health/readiness
curl -v http://127.0.0.1/health
curl -I http://127.0.0.1/
sudo nginx -t
sudo firewall-cmd --list-all
getenforce
sudo bash scripts/inspection.sh
```

从其他机器访问 `http://SERVER_IP/` 打开 NorthLedger，访问 `http://SERVER_IP/health` 检查入口；不应直接访问 18080。

## 9. 发布新版本与回滚

```bash
sudo bash scripts/deploy.sh --artifact /tmp/northledger-new.jar --web-dist /tmp/northledger-web-new --version 0.3.1
sudo bash scripts/rollback.sh
```

发布目录：

```text
/opt/opspilot/releases/<version>/opspilot.jar
/opt/opspilot/releases/<version>/web/index.html
/opt/opspilot/releases/<version>/web/assets/
/opt/opspilot/current -> 当前版本
/opt/opspilot/previous -> 上一版本
```

## 10. 卸载或清理

本Runbook不提供自动删除命令。实验结束时先列出systemd服务、目录、容器和卷，确认数据是否需要备份，再由你逐项决定，避免误删数据库卷。
