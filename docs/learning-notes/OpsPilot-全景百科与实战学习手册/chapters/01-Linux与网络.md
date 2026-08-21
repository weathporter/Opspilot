> [返回导航](../README.md) · [打开完整合订本](../OpsPilot-全景百科与实战学习手册.md)

# 第二篇：先把系统运行起来——Linux 与网络基础

## 第 5 章　Linux 不是命令表，而是应用运行的地基

### 5.1 先建立正确的 Linux 心智模型

你刚学完一遍 Linux 基础命令，最容易出现的误区是：记住了 `ls`、`cd`、`ps`、`grep`，却不知道它们为什么会在真实故障中一起出现。运维工作的对象不是命令，而是系统状态。命令只是观察和改变状态的工具。

对 OpsPilot 来说，一台 Linux 主机可以先抽象成七类对象：

1. 文件：JAR、Nginx 配置、环境文件、日志、备份都以文件存在。
2. 用户与权限：决定谁可以读密码、写日志、启动服务。
3. 进程：Java、Nginx、MySQL 都是正在运行的进程。
4. 端口与连接：进程通过监听端口接受网络请求，通过连接访问其他服务。
5. 服务管理：systemd 负责启动、停止、重启和记录服务状态。
6. 资源：CPU、内存、磁盘、inode、文件描述符会限制进程。
7. 日志与时间：排障必须把现象、版本、请求和系统事件放到同一时间线上。

当用户说“网站打不开”，不能立刻输入一堆命令。正确问题是：域名是否解析？网络是否可达？端口是否监听？Nginx 是否运行？Nginx 能否连接 Java？Java 是否就绪？Java 能否连接 MySQL？业务规则是否拒绝了请求？这条思维链比背二十条命令更重要。

### 5.2 OpsPilot 在 Rocky Linux 上的文件布局

原生部署把不同生命周期的数据分开：

```text
/opt/opspilot/
├─ releases/<version>/opspilot.jar   不可变版本制品
├─ current -> releases/<version>     当前版本软链接
├─ previous -> releases/<version>    回滚候选软链接
└─ shared/                            跨版本共享数据

/etc/opspilot/opspilot.env           运行配置和数据库凭据
/etc/systemd/system/opspilot.service 服务管理定义
/etc/nginx/conf.d/opspilot.conf       反向代理配置
/etc/logrotate.d/opspilot             日志轮转策略
/var/log/opspilot/application.log     应用日志
/var/backups/opspilot/mysql/          数据库逻辑备份
```

为什么不把所有东西都放到项目目录？因为代码、配置、日志和数据的生命周期不同。发布新 JAR 不应该覆盖密码；删除旧版本不应该删除日志；恢复数据库不应该依赖当前 JAR 目录；服务用户不应能随意修改 systemd 配置。

### 5.3 路径、软链接与发布版本

`current` 和 `previous` 是软链接。软链接可以理解为“指向另一个路径的路标”。systemd 永远从 `/opt/opspilot/current/opspilot.jar` 启动，但发布脚本只需把 `current` 原子地切换到新版本目录。

观察命令：

```bash
readlink -f /opt/opspilot/current
readlink -f /opt/opspilot/previous
ls -l /opt/opspilot
find /opt/opspilot/releases -maxdepth 2 -type f -name 'opspilot.jar' -print
```

预期现象：`readlink -f` 输出一个真实的版本目录；`ls -l` 能看到箭头；每个 release 目录只有自己的 JAR 和校验文件。

常见错误：

- `current` 指向不存在目录：systemd 的 `ExecStartPre` 检查失败。
- 当前用户没有穿越父目录的执行权限：即使 JAR 本身可读也打不开。
- 把新 JAR 直接覆盖 current 中的文件：更新过程中读者可能看到半写入制品，且旧版本不可审计。
- 在不同文件系统之间移动后误以为一定原子：原子重命名要求位于同一文件系统。

### 5.4 用户、组和最小权限

OpsPilot 使用不可登录的系统用户 `opspilot` 运行，而不是 root。原因是：应用一旦被利用，攻击者得到的权限应尽可能小。

需要区分三组权限：所有者、所属组、其他用户。`0640` 表示所有者可读写、组可读、其他用户无权限；`0750` 表示所有者可读写执行、组可读执行、其他用户无权限。

在项目中：

- `/etc/opspilot/opspilot.env` 建议 `root:opspilot 0640`：管理员修改，服务组读取。
- JAR 建议 `opspilot:opspilot 0640`：服务可读，不需要可执行位，因为由 `java -jar` 读取。
- 版本目录建议 `0750`：服务用户需要进入目录。
- 日志目录可由 `opspilot` 写入。

观察与验证：

```bash
id opspilot
namei -l /etc/opspilot/opspilot.env
stat /etc/opspilot/opspilot.env
sudo -u opspilot test -r /etc/opspilot/opspilot.env && echo readable
sudo -u opspilot test -w /etc/opspilot/opspilot.env || echo not-writable
```

`namei -l` 比只看文件本身更有价值，因为访问一个文件需要对路径上的每一级目录拥有适当权限。

### 5.5 进程、PID、前台与后台

进程是程序的一次运行实例。JAR 是磁盘上的文件，`java -jar` 启动后才产生进程和 PID。一个程序可以有多个进程；进程退出后，JAR 仍在。

```bash
ps -eo pid,ppid,user,%cpu,%mem,stat,etime,cmd | grep '[o]pspilot'
pgrep -a -f 'opspilot.jar'
```

需要读懂的字段：

- PID：当前进程编号，重启后通常改变。
- PPID：父进程编号；systemd 启动的 Java 由服务管理器跟踪。
- USER：运行身份，若显示 root 要追问为什么。
- STAT：进程状态；`R` 运行、`S` 可中断睡眠、`D` 不可中断等待、`Z` 僵尸。
- ETIME：已经运行多久，可辅助判断是否刚重启。
- CMD：真实启动参数，注意其中可能包含敏感信息，不应随便发送。

后台运行不等于服务化。手工在终端加 `&`，进程可能失去日志管理、重启策略和统一权限。systemd 才是 Linux 原生部署中可靠管理长期服务的入口。

### 5.6 端口、监听和连接

端口属于传输层的逻辑编号。`127.0.0.1:18080` 表示只在本机回环接口监听；`0.0.0.0:18080` 表示在所有 IPv4 接口监听。前者更适合原生部署中让 Java 只接受同机 Nginx 请求。

```bash
ss -lntp
ss -lntp '( sport = :80 or sport = :18080 or sport = :3306 )'
ss -antp | grep ':3306'
```

`-l` 看监听，`-n` 不做名称解析，`-t` 看 TCP，`-p` 尝试显示进程。监听存在只证明有进程占住端口，不证明接口逻辑正确。一个 Java 进程可能在监听，但 readiness 返回失败；Nginx 可能监听 80，但 upstream 配错导致 502。

### 5.7 日志、标准输出与 journal

原生部署中 systemd 能捕获进程标准输出和标准错误，通过 `journalctl` 查看；项目也可通过 `LOG_FILE` 写入 `/var/log/opspilot/application.log`，再由 logrotate 轮转。

```bash
journalctl -u opspilot -n 100 --no-pager
journalctl -u opspilot --since '10 minutes ago' --no-pager
tail -n 100 /var/log/opspilot/application.log
tail -F /var/log/opspilot/application.log
```

`tail -f` 按文件描述符跟随，文件被轮转替换后可能继续看旧文件；`tail -F` 会尝试按文件名重新打开，更适合日志轮转场景。项目使用 `copytruncate` 降低 Java 重开文件句柄的要求，但复制与截断瞬间理论上可能丢少量日志，这是简化方案的边界。

### 5.8 CPU、内存、磁盘与 inode

资源故障不能只看“使用率高”。要先区分现象、持续时间、影响和责任进程。

```bash
uptime
free -h
vmstat 1 5
df -hT
df -ih
ps -eo pid,user,%cpu,%mem,stat,etime,cmd --sort=-%cpu | head
```

基础解释：

- load average 不是 CPU 百分比，它表示可运行和不可中断任务的平均数量，需要结合 CPU 核数判断。
- Linux 会用空闲内存做缓存，不能看到 `free` 很小就断定内存不足；重点看 `available`、换页和 OOM 日志。
- `df -hT` 看字节容量，`df -ih` 看 inode。大量小文件会在容量未满时耗尽 inode，同样导致无法创建文件。
- 磁盘满时不要直接删除陌生文件。先定位增长来源、确认归属、选择轮转、归档、扩容或安全清理。

### 5.9 本章实验：从文件找到进程，再找到端口和日志

实验目标：你能把“JAR 文件、Java 进程、18080 监听、systemd 服务、日志”连成一条证据链。

1. 用 `readlink -f` 找当前 JAR。
2. 用 `systemctl status opspilot` 找主 PID。
3. 用 `ps` 确认 PID 的用户和启动命令。
4. 用 `ss` 确认 PID 监听 18080。
5. 用 `curl` 请求 readiness。
6. 用 `journalctl` 对齐本次启动时间。
7. 记录每一步正常输出的关键字段，而不是只写“命令成功”。

故障版本：把环境文件中的端口改成一个已占用端口，在实验机重启服务，观察 systemd 状态、Java 异常和监听结果。完成后恢复配置并重新验证。不要在保存现场前反复重启。

### 5.10 面试复述

“我排查一个 Linux 服务时不会先盲目重启。我先确认版本和影响，再按文件、权限、进程、监听、健康、日志、资源的顺序建立证据。OpsPilot 的 Java 由低权限用户通过 systemd 管理，只监听 127.0.0.1:18080，Nginx 监听 80。环境文件与 JAR 分离，日志由 journal 和 logrotate 管理，发布通过 current/previous 软链接保留回滚能力。”

### 5.11 本章验收题

1. JAR、镜像、容器和进程有什么区别？
2. 为什么 JAR 不需要 `chmod +x` 也能被 `java -jar` 运行？
3. 为什么文件权限正确，服务仍可能报 Permission denied？
4. `0.0.0.0:18080` 与 `127.0.0.1:18080` 有什么安全差异？
5. `df -h` 没满，为什么仍可能无法写日志？
6. 为什么“进程存在”和“业务可用”不是同一个结论？

## 第 6 章　systemd：让 Java 成为可管理的系统服务

### 6.1 systemd 解决什么问题

如果手工运行 `java -jar`，你需要自己处理后台运行、日志、崩溃重启、开机启动、运行用户、环境变量和停止信号。systemd 把这些要求写成声明式 Unit，并统一提供状态、日志和生命周期操作。

项目文件：`deploy/linux/systemd/opspilot.service`。

### 6.2 Unit、Service 和 Target

Unit 是 systemd 管理的对象；`.service` 管进程，`.target` 表示一组系统状态。OpsPilot 的 `WantedBy=multi-user.target` 表示启用后在常规多用户启动阶段被拉起。

`Wants=network-online.target` 表示希望网络就绪目标一起启动，但它不是“数据库已经可用”的保证；`After=`只控制启动顺序，也不等于健康依赖。真正的数据库连接仍要由应用启动、重试策略和健康检查负责。

### 6.3 逐项理解 OpsPilot Service

- `User/Group=opspilot`：限制运行身份。
- `WorkingDirectory=/opt/opspilot/current`：让相对路径基于当前版本目录。
- `EnvironmentFile=/etc/opspilot/opspilot.env`：把环境差异从 JAR 中分离。
- `ExecStartPre=/usr/bin/test -r ...`：在启动 Java 前快速拒绝不可读 JAR。
- `ExecStart=/usr/bin/java -jar ...`：前台运行，systemd 直接跟踪主进程。
- `Restart=on-failure`：异常退出才重启，人工 stop 不会被立刻拉起。
- `SuccessExitStatus=143`：SIGTERM 导致的常见退出状态视为正常停止。
- `TimeoutStopSec=30s`：给 Spring Boot 优雅停机时间，超时再强制结束。
- `NoNewPrivileges=true`、`ProtectSystem=strict`、`ProtectHome=true`：收紧进程对系统的权限。
- `ReadWritePaths`：只开放日志和共享目录写入。

### 6.4 reload、restart、enable 的区别

```bash
sudo systemctl daemon-reload
sudo systemctl enable opspilot
sudo systemctl start opspilot
sudo systemctl restart opspilot
sudo systemctl stop opspilot
systemctl status opspilot --no-pager
systemctl is-enabled opspilot
systemctl is-active opspilot
```

`daemon-reload` 让 systemd 重新读取 Unit 文件，不会自动重启业务；`reload` 通常让某个服务重读配置，前提是服务定义了 reload 行为；`restart` 停止再启动；`enable` 创建开机启动关系，不代表此刻已经运行。

### 6.5 如何读 systemctl status

重点不是绿色或红色，而是：Loaded 路径是否正确、Active 是 failed 还是 activating、Main PID 是谁、最近退出码是什么、重启次数是否异常、最后几行日志是什么。

常见状态：

- `active (running)`：进程存在，不等于 readiness 一定成功。
- `failed (Result: exit-code)`：看 `status=` 和 journal 中更早的根因。
- `activating (auto-restart)`：进程正在重启循环，应先停止循环、保存日志、定位原因。
- `start request repeated too quickly`：短时间失败过多触发限速，不应只执行 reset-failed 而不修根因。

### 6.6 优雅停机

systemd 停止时发送 SIGTERM。Spring Boot 配置 `server.shutdown=graceful`，会停止接收新请求，并在限定时间内让在途请求和事务完成。

这不意味着任何请求都能无限执行。外层 systemd 是 30 秒，Spring 生命周期配置为 20 秒，代理也有超时。优雅停机的目标是在可控时间内减少中断，不是永不强杀。

验证实验：发起一个可控的慢请求或观察普通请求流量，在另一个终端 `systemctl restart`，查看日志中停机与启动顺序。当前项目没有专门的慢接口，不能为了实验随意把生产式业务接口改成长睡眠；可以在隔离分支或测试配置中完成。

### 6.7 常见失败和处理

#### 环境文件不存在或权限错误

现象：服务在执行 Java 前失败，journal 提示无法读取 EnvironmentFile。检查 `systemctl cat opspilot`、`namei -l` 和 `stat`；修复路径与权限，再 daemon-reload（若 Unit 改动）和 restart。

#### Java 版本不兼容

现象：`UnsupportedClassVersionError`。检查 `java -version` 和构建目标。OpsPilot 要求 Java 17；生产主机只需兼容 JRE，不必安装 Maven。

#### 端口占用

现象：启动日志出现 Address already in use。用 `ss -lntp` 找占用进程，判断是旧实例、错误服务还是配置冲突。不要直接杀未知 PID。

#### 数据库不可达

现象：Flyway 或连接池初始化失败。检查环境文件、DNS/IP、3306 监听、TCP 连接、凭据和数据库日志。网络可达不代表账号权限正确。

#### 写日志失败

现象：Permission denied 或服务因只读保护无法写路径。确认日志目录在 `ReadWritePaths`，目录所有者和权限正确，磁盘与 inode 有空间。

### 6.8 你要亲手完成的验收

- 手动安装 Unit，执行 daemon-reload、enable、start。
- 改错一个无害配置，使服务失败；不看答案定位并恢复。
- 解释 `Restart=on-failure` 为什么不会在人工 stop 后立刻启动。
- 证明 Java 运行用户不是 root，且不能写 `/etc/opspilot`。
- 从 systemctl 主 PID 追到端口和 readiness。

## 第 7 章　Nginx：统一入口、反向代理与故障分界线

### 7.1 为什么浏览器不直接访问 Spring Boot

Nginx 作为入口有五个直接价值：

1. 对外只暴露稳定端口，后端端口可绑定本机或集群内部。
2. 统一提供 React 静态资源与 `/api` 反向代理，浏览器使用同源地址，减少跨域问题。
3. 隔离 `/actuator` 管理端点，只开放经过筛选的 `/health`。
4. 设置连接、发送和读取超时，避免无界等待。
5. 记录访问日志、传递真实来源和请求 ID，形成排障边界。

### 7.2 正向代理与反向代理

正向代理代表客户端访问外部目标，目标未必知道真实客户端；反向代理代表服务端接收客户端请求并选择后端。OpsPilot 的 Nginx 是反向代理。

客户端只知道 `localhost:18000` 或服务器 80 端口。Nginx 知道后端是 `opspilot-backend:18080`（容器/K8s）或 `127.0.0.1:18080`（原生 Linux）。

### 7.3 URL 路由

容器配置中：

- `/health` 精确匹配并代理到 `/actuator/health/readiness`。
- `/api/` 保留路径并代理到 Spring Boot。
- `/actuator/` 对外返回 404，避免暴露完整管理面。
- `/assets/` 是带内容哈希的静态资源，可长期缓存。
- 其他路径通过 `try_files` 回退 `index.html`，交给 React Router。

如果没有最后一条，用户直接刷新 `/transfers` 时，Nginx 会在文件系统中寻找这个目录并返回 404；但从首页点击进入可能正常，因为那次路由由浏览器内的 React 处理。

### 7.4 Host、X-Forwarded-* 与 X-Request-ID

`Host` 表示客户端希望访问的主机。`X-Real-IP` 和 `X-Forwarded-For` 保存代理前的来源链，`X-Forwarded-Proto` 表示原始协议。`X-Request-ID` 为一次请求建立关联标识。

项目曾遇到真实故障：健康检查直连 Java 为 200，经 Nginx 为 400。根因是 Nginx 把带下划线的 upstream 组名当成 Host 传给 Tomcat，而下划线不是合法域名字符。修复是对健康 location 显式设置 `proxy_set_header Host $host`。

这说明：看到 400 不能笼统说“后端挂了”。直连与代理结果的差异能快速把故障定位到代理请求构造。

### 7.5 502、504、404、400 分别意味着什么

- 502 Bad Gateway：Nginx 没有从 upstream 获得有效响应。常见于应用未监听、地址错误、容器旧 IP、连接被拒绝。
- 504 Gateway Timeout：已经尝试连接或等待后端，但在规定时间内没有完成。常见于慢 SQL、线程阻塞、连接池耗尽、网络黑洞。
- 404：可能是 Nginx 主动隐藏 `/actuator`，也可能是静态文件或后端路由不存在。必须看响应来源。
- 400：请求本身被拒绝，例如 Host 非法、JSON/参数不合法；它不同于 upstream 不可达。

### 7.6 Docker DNS 缓存故障

Compose 重建应用容器后可能分配新 IP。Nginx 若只在启动时解析服务名并缓存旧地址，会继续访问旧 IP，导致应用健康而 Nginx unhealthy。

项目配置使用 Docker 内置 DNS `127.0.0.11`，在 upstream 中启用动态 `resolve`。排障实验应比较应用容器重建前后的 IP、Nginx 健康状态和 DNS 有效期，而不是把容器 IP 写死。

### 7.7 配置变更的安全顺序

```bash
sudo nginx -t
sudo systemctl reload nginx
systemctl status nginx --no-pager
curl -v http://127.0.0.1/health
curl -v http://127.0.0.1:18080/actuator/health/readiness
```

先 `nginx -t`，再 reload。直接 restart 会让语法错误影响现有服务。验证时同时测 Java 直连和 Nginx 入口，才能判断问题在哪一层。

### 7.8 本章故障实验

实验一：把 upstream 端口临时改错。预期 Java 直连正常、Nginx 返回 502。检查 error log 后恢复并 reload。

实验二：临时去掉 SPA 的 `try_files` 回退。首页可能正常，直接刷新 `/transfers` 失败。说明浏览器路由和服务器文件查找的差异。

实验三：在隔离环境复现非法 Host，比较 400 日志。不要在共享环境长期保留错误配置。

### 7.9 面试复述

“我把 Nginx 当作外部请求和 Java 服务之间的故障分界线。OpsPilot 由 Nginx 同时提供 React 静态资源和 `/api` 反向代理，Java 在原生部署中只监听回环地址。排障时我会比较 Nginx `/health` 与 Java readiness：直连正常、入口 502 优先查 upstream；入口 504 查后端耗时；曾经还实际处理过 Host 头含下划线导致 Tomcat 400，以及容器重建后 Nginx 缓存旧 DNS 的问题。”

## 第 8 章　从浏览器到 MySQL：网络路径必须逐层说清

### 8.1 一次请求经过哪些层

以 Compose 中创建账户为例：

```text
浏览器 localhost:18000
  -> 宿主机端口映射
  -> Nginx 容器 8080
  -> Docker 网络 DNS 解析 opspilot-backend
  -> Spring Boot 容器 18080
  -> JDBC 解析 mysql
  -> MySQL 容器 3306
  -> 响应沿原连接返回浏览器
```

主机映射端口与容器端口不能混淆。浏览器访问 18000；Nginx 容器监听 8080。主机调试 MySQL 用 3307；应用容器必须访问 `mysql:3306`。在 app 容器中写 `localhost:3307` 意味着访问 app 容器自己，不是 MySQL 容器。

### 8.2 IP 地址与回环地址

IP 用于标识网络接口。`127.0.0.1` 是本机回环，只在当前网络命名空间内有效。Windows 主机的 localhost、Nginx 容器的 localhost、Java 容器的 localhost 是不同环境。

Docker 网络给容器提供隔离的网络命名空间和虚拟接口；Kubernetes Pod 也有自己的网络空间。服务名解析到哪个 IP，取决于调用者所在网络和 DNS 配置。

### 8.3 子网、默认网关与路由

子网掩码决定目标是否被认为在本地链路；若不在本地，数据包交给匹配的路由，通常最终走默认网关。排查远程服务器不可达时，应先看实际地址和路由，而不是套用示例 IP。

```bash
ip -br addr
ip route
ip route get <目标IP>
```

`ip route get` 能显示内核准备使用的出口接口、源地址和下一跳。它比只看 ping 更接近真实转发决策。

### 8.4 DNS：名字到地址

人使用 `mysql`、`opspilot-backend`、域名，连接最终需要 IP。Compose 提供服务名 DNS；Kubernetes Service 提供集群 DNS；主机域名通常由系统配置的 DNS 服务器解析。

DNS 成功只证明得到了地址，不证明目标端口开放；DNS 失败则根本还没进入 TCP 连接阶段。

```bash
getent hosts mysql
nslookup example.com
dig example.com
```

容器内部应在容器中执行解析检查。宿主机无法解析 Compose 内部服务名通常是正常边界，不应修改 hosts 文件让所有环境混在一起。

### 8.5 TCP 三次握手和端口状态

TCP 建立连接需要客户端发送 SYN、服务端返回 SYN-ACK、客户端再 ACK。常见现象：

- Connection refused：目标可达，但端口没有监听或被主动拒绝，通常很快失败。
- Timeout：可能被防火墙丢弃、路由错误、目标无响应，等待后超时。
- Reset：连接被对端或中间设备重置。

```bash
curl -v --connect-timeout 3 http://127.0.0.1:18080/actuator/health/readiness
nc -vz -w 3 127.0.0.1 3306
```

TCP 成功只证明能建立字节流，不证明 MySQL 用户密码正确，也不证明 HTTP 业务成功。

### 8.6 HTTP 请求和响应

HTTP 请求包含方法、路径、版本、请求头和可选正文；响应包含状态码、响应头和正文。

创建账户使用 POST，因为它产生服务器端资源；列表使用 GET；成功创建返回 201；参数错误返回 400；资源不存在返回 404；唯一冲突返回 409；业务规则冲突如余额不足返回 422。

```bash
curl -v -X POST http://localhost:18000/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -H 'X-Request-ID: learn-account-001' \
  -d '{"accountNo":"6222000000009001","holderName":"学习账户","openingBalance":1000.00}'
```

观察点：curl 输出中 `>` 是请求，`<` 是响应；记录状态码、`X-Request-ID` 和 JSON，不要只写“能访问”。

### 8.7 NAT 与端口映射

Compose 的 `18000:8080` 可以理解为宿主入口与容器入口的映射。外部连接宿主 18000，Docker 转发到 Nginx 容器 8080。容器之间在同一网络中直接使用服务名和容器端口，不绕主机映射。

端口映射不是进程监听本身。若容器内没有进程监听 8080，即使映射存在也无法服务；若进程只监听容器内 127.0.0.1，映射可能无法从容器接口访问。

### 8.8 防火墙、安全组与 NetworkPolicy

Linux firewalld 控制主机网络；云安全组通常在虚拟网络边界；Kubernetes NetworkPolicy 控制 Pod 流量；应用自身还可能有认证授权。它们是不同层。

排障不能一看到超时就说“防火墙问题”。需要证据：路由是否正确、目标是否监听、规则是否丢弃、同机是否能通、跨机是否失败、抓包是否看见 SYN/响应。

### 8.9 从外到内的网络排障顺序

1. 记录目标名称、IP、端口、协议和发生时间。
2. DNS：名字是否解析为预期地址。
3. 路由：系统准备从哪个接口和网关发包。
4. 端口监听：服务端是否真的在正确地址监听。
5. TCP：连接是拒绝、超时还是成功。
6. TLS（若有）：证书、SNI、有效期和信任链。
7. HTTP：状态码、响应头、代理日志。
8. 应用：参数、业务错误、线程与数据库。
9. 数据库：连接、认证、权限、锁和 SQL。

### 8.10 Wireshark/抓包应该看什么

你正在学计算机网络，不需要一开始就熟练分析所有协议。先学会回答：客户端是否发出 SYN？服务端是否回复？是否重传？三次握手后有没有 HTTP 请求？谁先发送 FIN/RST？

在 Linux 实验机可用 `tcpdump -nn -i any port 18080` 捕获最小流量；保存 pcap 后用 Wireshark 查看。抓包可能含敏感数据，必须在授权实验环境进行并妥善保管。

### 8.11 本章综合实验

#### 正常链路

依次观察：浏览器/主机 18000、Nginx 容器 8080、app 容器 18080、MySQL 3306；记录服务名解析和每段健康结果。

#### DNS 故障

在隔离 Compose 覆盖中把后端服务名改错。观察 Nginx 日志与 502，确认 app 容器本身仍健康，再恢复。

#### 端口错误

把代理目标端口改错，比较 Connection refused 与超时的速度和日志差异。

#### 应用错误

使用同一付款和收款账号发起转账。网络和 HTTP 链路仍可用，但服务返回 422。这证明“请求失败”不等于“网络失败”。

### 8.12 网络复述模板

“我按 DNS、路由、TCP、HTTP、应用、数据库逐层排查。Compose 中用户访问宿主 18000，映射到 Nginx 容器 8080；Nginx 通过 Docker DNS 访问 opspilot-backend:18080；Java 通过 mysql:3306 访问数据库。主机映射端口给宿主用，容器之间使用服务名和容器端口。连接拒绝、超时、502、504、422 分别代表不同层的证据，不能都归因于防火墙。”

## 第 9 章　Linux 第一响应：遇到故障时到底怎样思考

### 9.1 先保存现场，再决定是否重启

重启可能短暂恢复服务，却会改变 PID、连接、内存状态和部分日志上下文。正确顺序是：记录时间、版本、现象、影响范围，采集关键状态，然后根据业务影响选择修复、摘流、回滚或重启。

五分钟只读检查：

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

每条命令都要回答一个问题：时间线、负载、内存、磁盘容量、inode、监听、服务生命周期、应用日志、Java 直连、代理全链路。

### 9.2 症状到分层假设

#### 无法 SSH

先确认目标 IP、路由、云安全组、防火墙、22 端口和 sshd，而不是检查 OpsPilot Java 日志。

#### 80 端口拒绝

优先看 `ss`、Nginx 服务和监听地址。拒绝通常比被丢弃超时更快。

#### Nginx 502

直连 Java readiness；若直连也失败，继续查 Java/systemd/MySQL；若直连成功，查 Nginx upstream、DNS、Host、NetworkPolicy。

#### Nginx 504

关注请求耗时、数据库慢查询、锁等待、线程池/连接池和代理读超时。不要简单增大 timeout 掩盖根因。

#### 应用启动失败

查 journal 中最早的异常链，常见是密码、数据库不可达、Flyway 校验、端口占用、权限和 Java 版本。

#### 磁盘告警

区分容量与 inode，定位日志、数据库、容器镜像和临时文件增长。先停止无界增长，再选择轮转、归档、扩容或清理。

### 9.3 项目自带巡检与诊断包

`scripts/inspection.sh` 按主机资源、网络、服务、日志、配置和健康顺序输出；`scripts/collect-diagnostics.sh` 把主机、systemd、Nginx、journal、端口、进程以及可选 Docker 状态打包，并对环境文件只保留键名、隐藏值。

诊断包仍可能包含主机名、进程参数、业务日志和内部结构，发送给他人前必须人工检查。自动脱敏不是“绝对没有敏感信息”的保证。

### 9.4 故障处理的标准记录

每次演练使用以下结构：

```text
现象与影响
发现时间与当前版本
第一条证据
初始假设
执行的只读检查
被排除的原因
确认的直接根因和促成因素
临时恢复动作
永久修复
健康、业务和数据复验
预防措施与负责人
```

### 9.5 本篇总验收

- 能从浏览器画到 MySQL，标出每一段地址和端口。
- 能解释文件、进程、服务、监听和健康的区别。
- 能在 Nginx 502 时用两次 curl 把范围缩小。
- 能解释 systemd 的启用、启动、重启、reload、daemon-reload。
- 能完成一次端口占用、错误 upstream 或错误数据库密码的故障演练并写复盘。
- 能说明为什么保存现场比立即重启更专业。
