# Rocky Linux原生部署Runbook

目标：Spring Boot由systemd管理并只监听本机18080，Nginx监听80作为唯一入口。建议先在虚拟机或云实验机操作，不要直接用于真实生产主机。

## 1. 开发机完成构建

```bash
bash scripts/build.sh
sha256sum target/opspilot-0.1.0-SNAPSHOT.jar
```

企业环境通常在构建节点生成制品，生产主机只运行JRE，不安装Maven和完整源代码。

## 2. 复制引导文件与制品

```bash
scp -r deploy scripts linux-user@SERVER_IP:/tmp/opspilot-bootstrap/
scp target/opspilot-0.1.0-SNAPSHOT.jar linux-user@SERVER_IP:/tmp/opspilot.jar
```

## 3. 初始化Rocky Linux

```bash
ssh linux-user@SERVER_IP
cd /tmp/opspilot-bootstrap
sudo bash scripts/init-server.sh
```

脚本完成以下操作：安装JRE、Nginx和排障工具，创建`opspilot`服务用户，创建受控目录，安装systemd/Nginx/logrotate配置，处理firewalld和SELinux所需规则。

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

## 5. 配置应用

```bash
sudo vi /etc/opspilot/opspilot.env
sudo chown root:opspilot /etc/opspilot/opspilot.env
sudo chmod 0640 /etc/opspilot/opspilot.env
```

至少替换`DB_PASSWORD=CHANGE_ME`，并确认`DB_URL`与数据库位置一致。不要把真实密码放入仓库或命令历史。

## 6. 首次发布

```bash
cd /tmp/opspilot-bootstrap
sudo bash scripts/deploy.sh \
  --artifact /tmp/opspilot.jar \
  --version 0.1.0
```

## 7. 验证

```bash
systemctl status opspilot --no-pager
journalctl -u opspilot -n 100 --no-pager
ss -lntp | grep -E ':80|:18080|:3306'
curl -v http://127.0.0.1:18080/actuator/health/readiness
curl -v http://127.0.0.1/health
sudo nginx -t
sudo firewall-cmd --list-all
getenforce
```

从其他机器只访问`http://SERVER_IP/health`，不应直接访问18080。

## 8. 发布新版本与回滚

```bash
sudo bash scripts/deploy.sh --artifact /tmp/opspilot-new.jar --version 0.1.1
sudo bash scripts/rollback.sh
```

发布目录：

```text
/opt/opspilot/releases/<version>/opspilot.jar
/opt/opspilot/current -> 当前版本
/opt/opspilot/previous -> 上一版本
```

## 9. 卸载或清理

本Runbook不提供自动删除命令。实验结束时先列出systemd服务、目录、容器和卷，确认数据是否需要备份，再由你逐项决定，避免误删数据库卷。
