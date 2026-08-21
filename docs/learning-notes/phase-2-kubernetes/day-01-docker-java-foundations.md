# Day 1：Docker 对象模型与 Java 请求链

## 一、今天的完成标准

今天结束时，需要能够独立解释：

1. 镜像和容器有什么区别。
2. 为什么容器停止后镜像、网络和数据卷仍可能存在。
3. 主机 `18000:80`、`18080:18080`、`3307:3306` 分别表示什么。
4. Nginx 为什么能使用 `app:18080`，应用为什么能使用 `mysql:3306`。
5. MySQL 容器被重建后，数据为什么还能保留。
6. Compose 为什么先等 MySQL 健康，再启动应用。
7. 一次开户请求经过哪些 Java 类和容器。
8. `@Valid`、`@Transactional`、`saveAndFlush` 分别承担什么职责。
9. 为什么不能只看到 `Exited (137)` 就断定 Java 程序有 Bug。
10. Docker 与 Kubernetes 的学习关系是什么。

只要其中三项不能用自己的话说明，Day 1 就还没有完成。

---

## 二、不要先背命令：先认识 Docker 管理的对象

### 1. 镜像 Image

镜像是只读的应用模板。它包含：

- 基础文件系统；
- 运行时，例如 Java 17 JRE；
- 应用 JAR；
- 默认启动命令；
- 用户、工作目录等镜像元数据。

OpsPilot 的运行时镜像由 `Dockerfile.runtime` 构建：

```text
eclipse-temurin:17-jre-alpine
  + 非 root 用户 opspilot(10001)
  + /app/app.jar
  + java -jar /app/app.jar
  = opspilot-app 镜像
```

镜像本身不会处理 HTTP 请求。只有根据镜像创建并启动容器后，Java 进程才开始运行。

查看镜像：

```powershell
docker image ls
docker image inspect opspilot-app
```

### 2. 容器 Container

容器是镜像的一次运行实例。

可以粗略类比：

```text
Java class  → 镜像模板
Java object → 容器实例
```

这个类比不完全等价，但可以帮助入门：一个镜像可以创建多个容器，就像一个类可以创建多个对象。

查看正在运行的容器：

```powershell
docker ps
```

查看包括已停止容器在内的所有容器：

```powershell
docker ps -a
```

今天启动前，`docker ps` 只有 Minikube；`docker ps -a` 还能看到已经停止的 OpsPilot 容器。这证明：

```text
停止容器 ≠ 删除容器
删除容器 ≠ 删除镜像
删除容器 ≠ 删除命名数据卷
```

### 3. 网络 Network

容器默认有自己的网络命名空间和 IP。OpsPilot 使用自定义网络：

```text
opspilot_network
```

今天观察到的当前地址是：

```text
mysql  → 172.19.0.2
app    → 172.19.0.3
nginx  → 172.19.0.4
```

这些 IP 是运行时状态，容器重建后可能改变，因此配置不能依赖它们。

Docker 的内置 DNS 会把 Compose 服务名解析为当前容器 IP：

```text
mysql → 当前 MySQL 容器 IP
app   → 当前 Spring Boot 容器 IP
```

所以应用使用：

```text
jdbc:mysql://mysql:3306/opspilot
```

Nginx 使用：

```text
app:18080
```

这与 Kubernetes 后面的 Service/DNS 思想有关：调用方应该依赖稳定名称，不应依赖容易变化的实例 IP。

查看网络：

```powershell
docker network ls
docker network inspect opspilot_network
```

### 4. 数据卷 Volume

容器可写层与容器生命周期绑定。直接把 MySQL 数据只写在容器可写层，删除容器时数据就可能一起消失。

OpsPilot 使用命名卷：

```text
opspilot_mysql_data
```

并把它挂载到 MySQL 容器的：

```text
/var/lib/mysql
```

因此关系是：

```text
MySQL 容器可以被停止、删除和重建
                    │
                    └── 独立命名卷仍保存数据库文件
```

查看数据卷：

```powershell
docker volume ls
docker volume inspect opspilot_mysql_data
```

注意：数据卷可以提高容器重建后的数据持久性，但它不等于备份。磁盘损坏、误删数据或执行 `docker compose down -v` 时，仍然需要独立备份才能恢复。

### 5. Compose

Compose 用一份声明式配置描述多个容器及其关系。OpsPilot 基础环境包括：

```text
mysql
app
nginx
```

它不仅描述“启动三个容器”，还描述：

- 使用哪些镜像；
- 怎样构建应用镜像；
- 注入哪些环境变量；
- 加入哪个网络；
- 挂载哪个数据卷；
- 映射哪些端口；
- 使用什么健康检查；
- 按什么依赖顺序启动；
- 使用什么安全限制。

查看 Compose 解析后的服务：

```powershell
docker compose -f compose.yml -f compose.local.yml config --services
```

查看运行状态：

```powershell
docker compose -f compose.yml -f compose.local.yml ps
```

---

## 三、端口映射必须分清两端

Compose 中：

```yaml
ports:
  - "18000:80"
```

格式是：

```text
主机端口:容器端口
```

所以 `18000:80` 表示：

```text
访问 Windows localhost:18000
→ Docker 转发
→ Nginx 容器 80
```

OpsPilot 的三个映射：

| 主机访问 | 容器内部 | 用途 |
|---|---|---|
| `localhost:18000` | `nginx:80` | 统一业务入口 |
| `localhost:18080` | `app:18080` | 本机直接诊断应用 |
| `localhost:3307` | `mysql:3306` | 本机调试数据库 |

容器之间通信时不绕到主机映射端口。例如应用访问 MySQL 应使用：

```text
mysql:3306
```

而不是：

```text
localhost:3307
```

因为对应用容器而言，`localhost` 指的是应用容器自己，不是 Windows，也不是 MySQL 容器。

---

## 四、OpsPilot 请求到底怎样流动

一次请求：

```http
POST http://localhost:18000/api/v1/accounts
```

经过以下路径：

```text
PowerShell/浏览器
  │
  │ localhost:18000
  ▼
Docker 端口映射
  │
  │ nginx 容器 80
  ▼
Nginx location /
  │
  │ Docker DNS 解析 app
  │ app:18080
  ▼
Spring Boot/Tomcat
  │
  ▼
CorrelationIdFilter
  │
  ▼
DispatcherServlet
  │
  ▼
AccountController.create
  │
  ▼
AccountApplicationService.create
  │
  ▼
Account.open
  │
  ▼
AccountRepository.saveAndFlush
  │
  │ Docker DNS 解析 mysql
  │ mysql:3306
  ▼
MySQL account 表
  │
  ▼
AccountResponse → JSON → Nginx → 调用方
```

### 1. Nginx 层

Nginx 接收主机映射过来的请求，然后把普通业务请求代理给：

```text
app:18080
```

它还负责：

- 设置 `X-Request-ID`；
- 传递客户端地址和原始协议；
- 隔离 `/actuator/` 管理端点；
- 设置连接和读取超时；
- 定期重新解析 app 容器 IP。

### 2. CorrelationIdFilter

请求进入 Spring MVC 前，过滤器会处理 `X-Request-ID`：

- 合法上游 ID：继续使用；
- 缺少或不合法：生成 UUID；
- 写入响应头；
- 写入日志 MDC；
- 请求结束后清理 MDC，避免 Tomcat 线程复用时串号。

它解决的是“怎样把同一次请求的入口、日志、错误关联起来”。

### 3. AccountController

Controller 只负责 HTTP 协议层：

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public AccountResponse create(@Valid @RequestBody CreateAccountRequest request)
```

逐项理解：

- `@PostMapping`：匹配 POST 请求；
- `@RequestBody`：把 JSON 反序列化为 Java record；
- `@Valid`：执行 DTO 上的校验注解；
- `@ResponseStatus(CREATED)`：成功返回 201；
- 返回 `AccountResponse`：由 Jackson 序列化为 JSON。

Controller 不直接操作 Repository，避免 HTTP 协议、事务编排和领域规则混在一起。

### 4. CreateAccountRequest

它是输入 DTO，不是数据库实体。校验规则包括：

- 账号不能为空；
- 账号必须是 8～32 位数字；
- 姓名不能为空且最长 64 字符；
- 开户余额不能为负；
- 金额最多保留两位小数。

非法请求在进入数据库事务前就被拒绝，可以减少无意义的数据库操作。

### 5. AccountApplicationService

应用服务负责编排开户用例：

1. 创建 Account 领域对象；
2. 使用统一 Clock 获取时间；
3. 把时间截断到 MySQL 能保存的微秒精度；
4. 在事务中保存账户；
5. 捕获数据库唯一约束异常；
6. 转换为稳定的业务异常；
7. 把实体转换为响应 DTO。

`@Transactional` 表示这次用例在数据库事务中执行。方法正常结束时提交；运行时异常未被吞掉时回滚。

### 6. Account 领域实体

实体既映射 `account` 表，也保存账户自己的业务规则。

重要设计：

- 金额使用 `BigDecimal`，不使用 `double`；
- `status` 使用字符串枚举；
- `@Version` 提供乐观锁；
- 构造器受限，通过 `Account.open` 创建完整对象；
- 借记、贷记和状态校验放在实体内部。

### 7. AccountRepository

Repository 继承 `JpaRepository`，Spring Data 在运行时提供实现。

`saveAndFlush` 与单纯 `save` 的教学区别：

- `save` 后 SQL 可能延迟到事务提交时执行；
- `saveAndFlush` 要求尽快把 SQL 发给数据库；
- 因此唯一键冲突能在当前 `try/catch` 中暴露并转换。

### 8. AccountResponse

响应对象不直接暴露 JPA 实体。它隐藏：

- 数据库内部主键；
- 乐观锁版本号；
- 未来可能增加的内部字段。

这样数据库模型变化时，外部 API 契约不会被迫同步变化。

---

## 五、今天实际观察到的启动过程

执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

发生了四个阶段：

### 阶段 1：Maven 打包

```text
源代码和资源
→ 编译
→ 生成普通 JAR
→ Spring Boot repackage
→ 生成包含依赖的可执行 JAR
```

今天为了聚焦运行链路，使用了跳过测试的快速模式。跳过测试不是发布成功证明；正式验收要使用 `-RunTests`。

### 阶段 2：构建应用镜像

Docker 根据 `Dockerfile.runtime`：

1. 读取 Java 17 基础镜像；
2. 设置 `/app` 工作目录；
3. 创建 UID/GID 10001 的非 root 用户；
4. 复制 JAR；
5. 设置 `java -jar /app/app.jar` 入口。

今天大部分层命中缓存，所以构建很快。缓存命中不表示没构建，而是相同层无需重复执行。

### 阶段 3：按健康状态启动

```text
MySQL 启动
→ MySQL healthcheck 通过
→ Spring Boot 启动
→ Flyway 校验迁移
→ 应用 readiness 通过
→ Nginx 启动
→ 外部 /health 通过
```

Compose 的 `depends_on` 主要解决启动期顺序，不负责运行期高可用。MySQL 在运行过程中故障后，需要应用、监控和编排系统共同处理。

### 阶段 4：健康门禁

脚本从 Nginx 外部入口访问：

```text
http://localhost:18000/health
```

成功说明同时验证了：

- 主机端口；
- Nginx；
- Docker DNS；
- Spring Boot；
- readiness 端点。

---

## 六、健康、运行和业务正确不是同一件事

需要区分三个层次：

```text
容器 Running
≠ 容器 healthcheck 一定通过
≠ 业务功能一定正确
```

例如：

- Java 进程存在，所以容器 Running；
- 数据库连接失败，readiness 可能失败；
- readiness 成功，但转账代码仍可能存在业务错误；
- 所以还需要自动化测试和真实业务请求。

今天验证了：

```text
三个容器 Running + healthy
外部 /health 返回 UP
直接 readiness 返回 UP
真实开户成功
真实账户查询成功
```

这比只看 `docker ps` 更接近完整证据。

---

## 七、退出码不能脱离现场解释

今天启动前看到：

```text
Exited (143)
Exited (137)
Exited (0)
```

常见含义：

| 退出码 | 常见解释 | 是否能直接定责 |
|---|---|---|
| 0 | 进程正常结束 | 不能；长期服务不应无故结束 |
| 143 | 收到 SIGTERM 后结束 | 通常是受控停止，但仍需看时间和日志 |
| 137 | 收到 SIGKILL 或内存压力 | 不能；可能是强制关闭 Docker Desktop，也可能是 OOM |

判断时还要结合：

```powershell
docker inspect <容器>
docker logs <容器>
docker events
docker stats
```

运维判断的原则是：结论必须来自多项证据，而不是只背退出码表。

---

## 八、Docker 怎样过渡到 Kubernetes

今天学到的对象会继续演进：

| Docker/Compose | Kubernetes 方向 |
|---|---|
| 镜像 | 仍然使用容器镜像 |
| 容器 | Pod 中的 Container |
| Compose service | Deployment/StatefulSet + Service |
| Compose DNS 服务名 | Kubernetes Service DNS |
| `ports` | Service、Ingress、port-forward |
| environment | ConfigMap、Secret |
| 命名卷 | PV、PVC、StorageClass |
| healthcheck | startup/readiness/liveness Probe |
| restart policy | 工作负载控制器维持期望状态 |

Kubernetes 不是跳过 Docker，而是把“单机管理容器”扩展为“声明和协调一组工作负载”。

---

## 九、Day 1 常用命令分类

### 查看状态

```powershell
docker ps
docker ps -a
docker image ls
docker network ls
docker volume ls
docker compose -f compose.yml -f compose.local.yml ps
```

### 查看详情

```powershell
docker inspect opspilot-app-1
docker network inspect opspilot_network
docker volume inspect opspilot_mysql_data
```

完整 `docker inspect` 可能包含环境变量，分享输出前要检查是否含密码。

### 查看日志

```powershell
docker logs --tail 100 opspilot-app-1
docker logs -f opspilot-app-1
```

`-f` 会持续等待新日志，使用 `Ctrl+C` 退出观察，不会停止容器。

### 进入容器

```powershell
docker exec -it opspilot-app-1 sh
```

`exec` 是在已经运行的容器中启动额外进程，不是创建新容器。

### 查看资源

```powershell
docker stats --no-stream
```

### 停止与恢复

```powershell
docker compose -f compose.yml -f compose.local.yml stop
docker compose -f compose.yml -f compose.local.yml start
```

暂时不要执行带 `-v` 的删除命令；`-v` 可能删除 MySQL 数据卷。

---

## 十、本次实验中的编码现象

通过 PowerShell 发送中文开户人姓名时，终端响应显示了问号。当前能够确认的是：

- 请求成功进入应用；
- 数据库插入成功；
- 查询返回同一账户；
- 中文字符在发送或显示链路中发生了编码损失。

不能仅凭这一现象立刻断定 MySQL 字符集错误。排查应逐层确认：

```text
PowerShell 字符串
→ HTTP 请求体编码和 Content-Type charset
→ Jackson 解析结果
→ JDBC 参数
→ MySQL 列字符集
→ HTTP 响应编码
→ 终端显示编码
```

这个问题保留为后续练习，重点是学习“按层定位”，不是现在随意修改数据库配置。

---

## 十一、Day 1 验收方式

打开 [day-01-checkpoint.md](day-01-checkpoint.md)，先不看本讲义回答理解题，再完成观察实验。

验收标准：

- 10 道理解题至少 8 道能独立解释；
- 能画出三容器请求路径；
- 能区分主机端口和容器端口；
- 能说明容器、镜像、网络、卷的生命周期差异；
- 能从 Java 类定位开户请求的协议、事务、持久化和响应转换位置；
- 能用至少三项证据说明 OpsPilot 当前健康。
