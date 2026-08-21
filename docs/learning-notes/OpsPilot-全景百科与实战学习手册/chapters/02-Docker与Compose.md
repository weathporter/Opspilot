> [返回导航](../README.md) · [打开完整合订本](../OpsPilot-全景百科与实战学习手册.md)

# 第三篇：容器不是魔法——Docker 与 Compose

## 第 10 章　Docker 的对象模型：镜像、容器、网络和卷

### 10.1 为什么先学对象，不先背命令

Docker 命令的第一个参数通常就在告诉你正在操作什么对象：image、container、network、volume、compose。只要对象关系清楚，命令只是“查看、创建、启动、停止、删除、检查”的不同组合；如果对象关系不清，遇到一个报错就会反复重建，甚至误删数据卷。

OpsPilot 的 Compose 基础环境包含三个业务服务：MySQL、Spring Boot API、React/Nginx Web。可观测覆盖再增加 Prometheus、Alertmanager、Grafana、Loki、Alloy、Node Exporter、cAdvisor，共十个容器。

### 10.2 镜像是什么

镜像是只读的分层模板，包含程序、运行时、依赖、默认配置和启动命令。它更接近“可交付软件包”，不是正在运行的进程。

```bash
docker image ls
docker image inspect opspilot-api:dev
docker history opspilot-api:dev
```

需要观察：仓库名、标签、镜像 ID、创建时间、大小、架构、入口命令、用户和层。标签是可变名字，镜像 ID/摘要更接近内容身份；生产发布不应长期只使用 `latest`，否则很难证明线上究竟是哪一版。

### 10.3 容器是什么

容器是镜像的一次运行实例，加上可写层、网络、资源限制和进程状态。同一个镜像可以创建多个容器；删除容器不会自动删除镜像；重建容器会得到新的容器身份和可写层。

```bash
docker ps
docker ps -a
docker inspect <container>
docker top <container>
docker logs --tail 100 <container>
```

`docker ps` 默认只看运行容器；`docker ps -a` 还包括已退出容器。容器退出不是“消失”，退出码、结束时间和日志仍可用于判断。

### 10.4 容器 Running、healthy 与业务正确

这是必须说清的三层状态：

- Running：容器主进程还存在。
- healthy：Dockerfile/Compose 定义的健康命令连续通过。
- 业务正确：真实业务请求得到正确结果，数据和审计记录一致。

Java 进程可能 Running，但数据库密码错误导致 readiness 失败；Nginx 可以 healthy，但某个不在健康检查中的页面功能仍有 Bug；所有容器都 healthy，也不能证明幂等和转账余额正确。因此验收要从进程逐步走到端到端业务。

### 10.5 退出码怎样解释

- 0：进程正常结束，但长期服务不应无故结束。
- 1：通用应用错误，需要看日志。
- 126/127：命令不可执行或找不到。
- 137：进程收到 SIGKILL，可能是 OOM Kill，也可能人工 `kill -9` 或超时强杀，不能只凭数字断言 OOM。
- 143：进程收到 SIGTERM，常见于正常 stop，项目把原生 systemd 中的 143 视为成功停止。

验证 137 是否 OOM 还要看容器状态中的 `OOMKilled`、内核/运行时事件、资源限制和内存趋势。

### 10.6 Docker 网络

Compose 创建 `opspilot_network`。连接到同一网络的容器可以使用服务名进行 DNS 解析：Nginx 访问 `opspilot-backend:18080`，Java 访问 `mysql:3306`。

```bash
docker network ls
docker network inspect opspilot_network
docker exec <app-container> getent hosts mysql
docker exec <nginx-container> getent hosts opspilot-backend
```

不要把 `172.x.x.x` 容器 IP 写死。容器重建后 IP 可能变化，服务名才是稳定契约。

### 10.7 Docker 数据卷

容器可写层随容器删除而消失。MySQL 数据必须写入命名卷 `opspilot_mysql_data`，这样容器重建后数据仍存在。

```bash
docker volume ls
docker volume inspect opspilot_mysql_data
```

卷持久化不等于备份：误删除、逻辑错误和磁盘损坏仍会影响卷。备份需要独立副本、完整性校验和恢复演练。

### 10.8 绑定挂载与命名卷

绑定挂载把宿主具体路径映射进容器，适合配置文件与开发源码；命名卷由 Docker 管理，适合数据库和时序数据。OpsPilot 把 Prometheus、Grafana、Loki 等配置以只读绑定挂载，把运行数据放命名卷。

Windows Docker Desktop 实际通过 Linux VM 运行容器，所以 Linux 根文件系统挂载和 `rslave` 传播与原生 Rocky Linux 不完全一致。项目用 `compose.desktop-observability.yml` 单独覆盖 Node Exporter 挂载，而不是污染通用 Compose 文件。

### 10.9 本章实验与验收

1. 找出 API 镜像和容器，解释二者 ID、生命周期和可写层区别。
2. 停止 app 容器，比较 `docker ps` 与 `docker ps -a`。
3. 查看容器退出码和日志，再启动恢复。
4. 记录 app、nginx、mysql 的服务名与容器 IP，重建 app 后比较 IP 是否变化。
5. 重建 MySQL 容器但保留卷，验证账户数据仍在。
6. 解释为什么这仍然不是一次数据库恢复演练。

## 第 11 章　Dockerfile：从源码构建安全、可运行的镜像

### 11.1 构建上下文

执行 `docker build` 时，Docker 客户端把构建上下文发送给构建引擎；`COPY` 只能访问上下文内且未被 `.dockerignore` 排除的文件。上下文过大将拖慢构建并可能泄漏无关文件。

OpsPilot 完整后端 Dockerfile 以项目根为上下文，先复制 pom，再复制 src。前端 Dockerfile 同样先复制 package 清单，再复制前端源码，以利用层缓存。

### 11.2 多阶段构建

后端第一阶段使用 Maven + JDK 编译；第二阶段只使用 JRE 运行。前端第一阶段用 Node 构建静态资源；第二阶段只保留 Nginx 和 dist。

好处：

- 最终镜像不携带 Maven、Node、源码和构建缓存，体积和攻击面更小。
- 构建环境统一，CI 或新机器只需 Docker。
- 构建与运行职责分离。

本地还提供 `Dockerfile.runtime`，复用主机已经生成的 JAR/dist，用于镜像网络不稳定时减少构建阶段拉取依赖。它是本地加速路径，不替代正式多阶段构建。

### 11.3 分层缓存

Docker 每个指令形成可缓存层。先复制依赖清单并下载依赖，再复制常变化源码，可以在 pom/package-lock 不变时复用依赖层。

缓存不是正确性的保证。如果依赖源、构建参数或基础镜像变化，需要理解缓存键和必要时进行无缓存验证，而不是遇到奇怪结果就永久使用 `--no-cache`。

### 11.4 非 root 用户

后端固定 UID/GID 10001，前端使用 Nginx 用户 101。容器内 root 也受命名空间等限制，但仍比普通用户权限大；以非 root 运行能减少漏洞后的破坏范围，并与 Kubernetes `runAsNonRoot` 对齐。

验证：

```bash
docker exec <app-container> id
docker exec <nginx-container> id
docker inspect <app-container> --format '{{.Config.User}}'
```

### 11.5 只读根文件系统与 tmpfs

Compose 对 app/nginx 设置 `read_only: true`。应用不能随意写镜像根文件系统；必须写的临时路径用 tmpfs 提供。Kubernetes 中相同思想通过 `readOnlyRootFilesystem` 和 `emptyDir` 实现。

常见故障：程序或库默认写 `/tmp`、Nginx 写缓存/运行目录、Java 配置写日志文件。解决方法不是取消所有只读保护，而是识别合法可写目录，最小化挂载。

### 11.6 ENTRYPOINT 的 exec 形式

`ENTRYPOINT ["java", "-jar", "/app/app.jar"]` 不经过 shell，让 Java 成为 PID 1，能直接收到 Docker stop 的 SIGTERM。若写成 shell 字符串，信号可能先到 shell，优雅停机行为更复杂。

### 11.7 JVM 容器内存与 OOM

`-XX:MaxRAMPercentage=75.0` 让 JVM 按可见内存预算堆等内存，给线程栈、元空间、本地内存留余地；`ExitOnOutOfMemoryError` 让无法健康运行的 JVM 退出，由编排系统重建。

这不是通用最优值。生产需根据堆、非堆、线程、直接内存和容器 limit 通过压测确定。只有 limit 没有 request，或 request/limit 与 HPA 指标不合理，都可能造成调度与扩容问题。

### 11.8 镜像元数据与版本

构建参数 `APP_VERSION` 写入 OCI Label，同时运行时环境把版本暴露到 Actuator info 和 Prometheus 公共标签。理想状态下，制品、镜像标签、应用 info、发布记录和 Git commit 能互相对应。

当前仓库尚无正式 Git 提交，因此版本追溯链还不完整。补齐 Git 基线和 CI 构建元数据是比新增热门组件更优先的改进。

### 11.9 健康检查

后端镜像访问 `127.0.0.1:18080/actuator/health/readiness`；前端镜像访问本地 Nginx `/health`，后者还会代理到 API readiness。因此前端健康检查实际覆盖 Nginx -> API 的链路。

健康检查参数含义：interval 是周期、timeout 是单次上限、retries 是连续失败阈值、start-period 是启动宽限。设置过严会在 Flyway/MySQL 初始化时误判，过松会延迟故障发现。

### 11.10 镜像构建排障

#### 拉取基础镜像失败

先区分标签不存在、DNS 失败、代理/镜像源失败、TLS 或连接中断。项目曾遇到不可用 registry mirror DNS；没有因此修改项目成假镜像名，而是保留官方镜像和处理机器级配置。

#### Maven/npm 下载失败

检查网络、代理、仓库、证书、锁文件和缓存。不要把本地 `node_modules` 粗暴复制进 Linux 镜像。

#### COPY 找不到文件

检查当前上下文、Dockerfile 路径、`.dockerignore` 和文件是否在构建前生成。

#### 容器启动立即退出

构建成功只说明镜像生成。继续看 ENTRYPOINT、运行用户权限、环境变量、端口、Java 异常和架构兼容性。

## 第 12 章　Docker Compose：单机上的完整系统编排

### 12.1 Compose 解决什么问题

单独运行十次 `docker run` 很难保证网络、卷、环境、启动依赖和命名一致。Compose 用 YAML 声明多个服务，创建项目级网络和卷，并让启动方式可复现。

Compose 适合本地开发、单机实验和小型部署；它不提供 Kubernetes 那样的跨节点调度、控制器自愈和集群级资源治理。

### 12.2 文件叠加

OpsPilot 通过多个文件分离职责：

- `compose.yml`：MySQL、API、Web 的基础业务拓扑。
- `compose.local.yml`：选择复用主机构建产物的 runtime Dockerfile。
- `compose.observability.yml`：增加七个观测组件。
- `compose.desktop-observability.yml`：只处理 Windows Docker Desktop 挂载兼容。

后面的文件覆盖或扩展前面的服务。排障时必须记录实际使用了哪些 `-f`，否则“同一个项目”可能得到不同结果。

### 12.3 MySQL 服务

MySQL 8.4 容器创建数据库和应用用户；宿主只绑定 `127.0.0.1:3307`，避免与常用 3306 冲突；容器间仍用 3306。数据写入命名卷，健康检查用 root 密码执行 `mysqladmin ping`。

环境变量只在第一次初始化空数据目录时创建库和用户。已有数据卷中修改 `MYSQL_PASSWORD` 不会自动更新数据库账户密码，这是常见误解。

### 12.4 API 服务

API 环境把 `DB_URL` 覆盖为 `jdbc:mysql://mysql:3306/opspilot...`。`depends_on` 等 MySQL healthy 后再启动，减少初始化竞态；但运行期间 MySQL 故障不会由 `depends_on` 自动处理。

API 对主机只绑定 `127.0.0.1:18080` 作为调试口，对外用户应走 Nginx 18000。服务还有 `opspilot-backend` 网络别名，使同一前端镜像在 Compose 与 Kubernetes 都访问同一个后端名称。

### 12.5 Web 服务

Web 把宿主 18000 映射到容器 8080。它等待 app healthy，再启动 Nginx；健康检查访问 `/health`，所以既验证静态入口进程，也验证到 API readiness 的代理链。

### 12.6 depends_on 的边界

它主要解决启动顺序，不是运行期服务编排器。MySQL 后来断开，Compose 不会因为依赖关系自动重启 API；应用需要正确处理连接失败，监控需要发现，操作者或 restart policy 决定恢复。

“容器 A 在 B 之后启动”也不等于 B 的全部业务已经准备好，所以项目使用 health condition，而不是只看进程 started。

### 12.7 restart policy

`unless-stopped` 表示容器异常退出或 Docker 重启时尝试恢复，但人工明确停止后保持停止。重启策略能提高恢复性，却可能让错误配置形成重启循环；此时要看第一次失败日志，而不是只看不断变化的容器状态。

### 12.8 端口与暴露面

| 宿主地址 | 作用 | 是否给普通用户 |
| --- | --- | --- |
| `localhost:18000` | Web/Nginx 唯一业务入口 | 是，本地演示 |
| `localhost:18080` | API 直连调试 | 否，仅排障 |
| `localhost:3307` | MySQL 本机调试 | 否 |
| `localhost:13000` | Grafana | 是，本地运维演示 |
| `localhost:19090` | Prometheus | 仅运维 |
| `localhost:19093` | Alertmanager | 仅运维 |
| `localhost:13100` | Loki 健康/接口 | 仅运维 |

多个端口绑定 127.0.0.1，意味着只接受本机连接；Nginx 18000 当前绑定所有主机接口，使用公共网络时应结合防火墙和访问控制。

### 12.9 一键启动脚本实际做了什么

`scripts/start-local.ps1` 不是“神奇的一键运行”。它按顺序完成：

1. 读取当前 Docker context 的 Engine 端点。
2. 可选运行真实 MySQL Testcontainers 测试，否则跳过测试打包 JAR。
3. 安装前端依赖（首次）并执行 TypeScript/Vite 生产构建。
4. 选择 Compose 文件；可选追加观测栈和 Desktop 覆盖。
5. 构建 runtime 镜像并后台启动。
6. 最多约 90 秒轮询 Nginx `/health`。
7. 失败时输出容器状态并以非零结果结束；成功时显示入口。

脚本的价值是把成功标准写成可重复门禁，不是让你永远不理解 Maven、npm、Compose 和健康检查。

### 12.10 正确启动方式

基础业务：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

含观测栈：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability
```

含真实数据库测试：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1 -WithObservability -RunTests
```

启动前确认 Docker Desktop Engine 已运行。Desktop 界面打开不一定代表 Linux Engine 已完全就绪。

### 12.11 停止、删除与数据风险

```bash
docker compose -f compose.yml -f compose.local.yml down
```

这会删除容器和项目网络，但保留命名卷。添加 `-v` 会删除 Compose 卷，包括 MySQL、Prometheus、Grafana 和 Loki 数据。除非明确需要清空并已经备份，否则不要随手执行 `down -v`。

`stop` 只停止容器，`start` 可恢复已有容器；`down` 拆除容器和网络；`rm` 删除已停止服务容器。要根据目标选择，不要把“重启”一律做成删除重建。

### 12.12 Compose 排障顺序

```bash
docker compose -f compose.yml -f compose.local.yml ps
docker compose -f compose.yml -f compose.local.yml logs --tail 100 app
docker inspect <container>
docker network inspect opspilot_network
docker volume inspect opspilot_mysql_data
docker stats --no-stream
```

先看服务状态和 health，再看目标服务日志；inspect 核对环境、挂载、端口、网络和退出状态；network/volume 分别证明连通关系和数据位置；stats 是瞬时资源快照，不替代持续监控。

## 第 13 章　Docker 常见故障百科

### 13.1 无法连接 Docker Engine

现象可能包括 named pipe 不存在、daemon not running、context 指向错误、配置文件权限拒绝。先区分 Docker CLI 是否存在与 Engine 是否运行。

检查：`docker context ls`、`docker context inspect`、`docker info`。Windows 上还要看 Docker Desktop 的 Engine 状态和 WSL/虚拟化环境。

本次编写手册时，受限执行会话无法读取用户 Docker/Kubernetes 配置，且指定 Linux Engine pipe 未存在，因此不能把 2026-08-11 的健康结果说成此刻仍在线。这是权限/当前状态的证据边界，不等于代码已损坏。

### 13.2 端口已经被占用

Compose 报 bind failed。先用主机工具找 18000/18080/3307 等端口的占用者，确认是否是旧项目、IDE、代理或其他数据库。项目已经主动避开常见 8080，但任何端口仍可能被占用。

修复可以是停止冲突服务或有计划地修改宿主映射。容器内部端口通常不必跟着改；若改入口端口，还要同步文档、脚本、健康检查和前端链接。

### 13.3 MySQL unhealthy

看 MySQL 日志、健康命令、数据卷权限、初始化时间和密码变量。首次启动可能较慢；已有卷与新环境变量密码不一致也是常见原因。不要直接删卷“解决”，除非确认数据可丢弃。

### 13.4 API 反复重启

看第一次启动日志。高概率原因：MySQL 不可达、认证失败、Flyway 校验失败、JAR 缺失/错误、只读路径写入、内存不足。`depends_on` 健康只保证启动那一刻 MySQL ping 成功，不保证应用账号和迁移一定成功。

### 13.5 Nginx unhealthy，API healthy

优先查 Nginx 到 `opspilot-backend:18080` 的 DNS、端口、Host 头和代理日志。项目已经处理动态 DNS，但要确认运行镜像使用最新配置，旧容器可能仍持有旧文件。

### 13.6 页面能开但没有数据

静态资源正常不代表 API 正常。浏览器开发者工具看 `/api` 状态；从 Nginx 入口 curl；再直连 API；查看后端日志和 MySQL。可能是 502、业务 4xx、JSON 契约不一致、数据库为空或前端错误。

### 13.7 镜像拉取失败

依次判断：名称/标签、DNS、镜像源、代理、证书、限流、平台架构。不要因为某个镜像源失败就随意替换成不可信镜像。Minikube 中自研镜像可用 `minikube image load`，但基础 MySQL 等镜像也要确保集群内可用。

### 13.8 容器时间与日志时间不一致

检查主机时间、时区、容器 `TZ`、JVM/JDBC 时区和浏览器显示。OpsPilot 统一使用 Asia/Shanghai 并把数据库时间精度截断到微秒，但生产中更常见的策略是存 UTC、展示按用户时区，需要全链统一设计。

### 13.9 磁盘被 Docker 占满

先用 `docker system df`、卷和日志文件定位。镜像、build cache、容器 JSON 日志、MySQL、Prometheus、Loki 都可能增长。清理前确认对象是否仍引用、数据是否需要保留。禁止把 `docker system prune -a --volumes` 当日常排障万能命令。

### 13.10 本篇复述与验收

复述：

“OpsPilot 用多阶段镜像把构建工具留在 builder 阶段，运行阶段只保留 JRE 或 Nginx；容器以非 root、只读根文件系统运行。Compose 把 MySQL、API、Web 和观测组件放在同一项目网络，容器间通过服务名和容器端口访问，宿主端口只用于外部入口。MySQL 使用命名卷，但卷不等于备份。启动脚本执行构建、编排和健康门禁，排障仍按容器状态、日志、inspect、网络、卷和业务请求逐层进行。”

验收：

- 不看答案说出四个 Docker 核心对象。
- 解释 `18000:8080`，并说明 app 为什么连接 `mysql:3306`。
- 从 Dockerfile 找到非 root、只读文件系统配套、健康检查和 PID 1 设计。
- 重建 app 容器，验证 Nginx 仍能在 DNS 更新后访问。
- 停止/启动/拆除环境，但不丢 MySQL 卷。
- 解释为什么 137 不能直接证明 OOM。
