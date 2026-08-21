# OpsPilot 三节点 Kubernetes 实战记录（2026-08-18）

> 文档性质：当天实战过程、证据、故障与知识边界的独立记录。
>
> 项目目录：`D:\Develop\OpsPilot`
>
> 集群类型：1 个控制平面节点 + 2 个工作节点的 VMware 学习集群。
>
> 安全说明：本文不记录数据库密码明文。实验使用的 Secret 值由操作者交互式输入，仅记录对象名称和字段名称。

## 1. 今日结论

今天已经把 OpsPilot 从“本机可构建的 Java/React 项目”推进为“运行在三节点 Kubernetes 集群中的完整业务系统”，并完成了以下闭环：

1. 后端、前端和 MySQL 镜像在 Windows 主机完成构建与测试。
2. 三个镜像被打包成离线文件，校验后分别导入两个工作节点。
3. 在 Kubernetes 中部署了 Web、API、MySQL、Service、PV/PVC、ConfigMap、Secret、PDB 和 NetworkPolicy 等对象。
4. 两个工作节点都能通过 NodePort 访问前端。
5. 从 `.101` 入口写入的账户可以从 `.102` 入口读取，证明两个入口背后使用同一套服务与数据库。
6. 完成真实转账，验证余额变化、借贷双流水和总金额守恒。
7. 主动删除一个 API Pod，Deployment 自动创建新 Pod 并恢复 Ready，完成一次基础自愈实验。
8. 定位了部署初期 API 重启、预检脚本误判和中文幂等键三个真实问题。

但这还不能称为“生产级 Kubernetes”或“深度掌握 Kubernetes”。准确定位是：

> 已完成真实三节点部署、业务闭环和基础自愈的第一阶段实战；高可用、弹性伸缩、可观测性、发布治理、备份恢复和现代化集群仍需继续完成。

---

## 2. 今日目标与验收标准

### 2.1 目标

将 `D:\Develop\OpsPilot` 中的 Spring Boot、React 和 MySQL 系统部署到三节点 Kubernetes 学习集群，并用业务和故障实验验证它确实在工作。

### 2.2 验收标准

| 验收项 | 预期结果 | 今日结果 |
| --- | --- | --- |
| 三节点状态 | master、node1、node2 均为 Ready | 已验证 |
| 本地测试 | Java 自动化测试通过，前端可构建 | 已验证 |
| 离线镜像 | 两个工作节点均存在 API、Web、MySQL 镜像 | 已验证 |
| 工作负载 | MySQL 1 个副本，API 2 个副本，Web 2 个副本 | 已验证 |
| 跨节点访问 | `.101:30080` 与 `.102:30080` 均可访问 | 已验证 |
| 数据一致性 | 两个入口看到相同账户、余额和流水 | 已验证 |
| 转账事务 | 扣款、入账、余额与双流水正确 | 已验证 |
| 幂等性 | 相同幂等键重复请求不重复扣款 | 已验证 |
| 基础自愈 | 删除一个 API Pod 后自动补齐并恢复 Ready | 已验证 |
| MySQL 持久化 | 删除 MySQL Pod 后数据仍存在 | 尚未实验 |
| 滚动发布与回滚 | 更新镜像并验证无中断与回滚 | 尚未实验 |
| PDB/NetworkPolicy | 通过 drain 和网络故障实验验证效果 | 尚未实验 |

---

## 3. 环境基线

### 3.1 Windows 开发主机

| 项目 | 当前环境 |
| --- | --- |
| 项目位置 | `D:\Develop\OpsPilot` |
| Docker | Docker Desktop，客户端/服务端 29.4.0 |
| Docker Context | `desktop-linux` |
| Java | Java 17 |
| 后端 | Spring Boot 3.5.4 |
| 前端 | React、Vite、Nginx |
| 数据库镜像 | MySQL 8.4 |

### 3.2 VMware Kubernetes 集群

| 主机 | IP | 角色 | 今日主要操作位置 |
| --- | --- | --- | --- |
| `k8s-master` | `192.168.99.100` | control-plane、master | 执行 `kubectl`、创建 Secret、部署和观察集群 |
| `k8s-node1` | `192.168.99.101` | worker | 导入镜像、创建 MySQL 本地数据目录、承载部分 Pod |
| `k8s-node2` | `192.168.99.102` | worker | 导入镜像、承载部分 Pod |

集群软件基线：

| 项目 | 版本/状态 |
| --- | --- |
| Kubernetes | v1.21.10 |
| 操作系统 | CentOS Linux 7 (Core) |
| 内核 | 3.10.0-1160.el7.x86_64 |
| 容器运行方式 | Docker 20.10.8，dockershim 时代方案 |
| CNI | Calico |
| PDB API | `policy/v1` 可用 |

### 3.3 版本边界

这套集群适合复现 Kubernetes 核心对象和调度、存储、网络、自愈等原理，但属于旧版学习环境：

- Kubernetes v1.21 已停止维护。
- CentOS 7 已结束常规生命周期。
- Kubernetes 当前生产环境通常使用 containerd 或 CRI-O，而不是 dockershim。
- 当前只有一个控制平面节点，不具备控制面的高可用能力。

因此，本文中的结论必须写成“旧版三节点实验集群实操”，不能写成“当前生产集群标准方案”。后续还要在受支持版本的 Kubernetes 和 CRI 运行时上再走一遍现代化路线。

---

## 4. 系统架构与请求链路

```text
Windows 浏览器
  │
  ├── http://192.168.99.101:30080
  └── http://192.168.99.102:30080
               │
               ▼
      NodePort Service: opspilot-web
               │
               ▼
       Web Deployment（2 副本）
        Nginx + React 静态文件
               │  /api 反向代理
               ▼
   ClusterIP Service: opspilot-backend:18080
               │
               ▼
       API Deployment（2 副本）
           Spring Boot 3.5.4
               │  JDBC
               ▼
       Headless Service: mysql:3306
               │
               ▼
       MySQL StatefulSet（1 副本）
               │
               ▼
 PVC opspilot-mysql-data（5 Gi，RWO）
               │
               ▼
 PV opspilot-mysql-pv（local，Retain）
               │
               ▼
 k8s-node1:/data/opspilot/mysql
```

关键理解：

- 浏览器访问的是节点 IP 和 NodePort，不是某个固定 Web Pod。
- Service 根据标签选择 Ready Pod，为 Pod IP 的变化提供稳定入口。
- Web Pod 不直接访问 MySQL，只访问后端 Service。
- API Pod 通过名为 `mysql` 的 Service 找到 MySQL。
- MySQL 数据实际落在 node1 的本地磁盘目录。
- API 和 Web 是双副本；MySQL 仍是单副本，因此只有数据持久化，不是数据库高可用。

---

## 5. 项目组件与 Kubernetes 对象映射

| 项目组件 | Kubernetes 对象 | 副本 | 暴露方式 | 主要作用 |
| --- | --- | ---: | --- | --- |
| React/Nginx 前端 | Deployment + Service | 2 | NodePort `30080` | 页面入口与 `/api` 反向代理 |
| Spring Boot API | Deployment + Service | 2 | ClusterIP `18080` | 账户、转账、流水和健康检查 API |
| MySQL | StatefulSet + Headless Service | 1 | 集群内 `3306` | 业务数据存储 |
| MySQL 数据 | PV + PVC | 5 Gi | local PV | 将数据保存到 node1 本地目录 |
| 普通配置 | ConfigMap | - | Pod 环境变量 | 环境名、数据库地址、版本等非敏感配置 |
| 数据库凭据 | Secret | - | Pod 环境变量 | 数据库用户密码和 root 密码 |
| API/Web 可用性约束 | PDB | - | `policy/v1` | 为主动驱逐设定最低可用性约束 |
| 组件间通信约束 | NetworkPolicy | - | Calico | 限制进入各组件的网络流量 |
| 多节点分布 | node label + topology spread | - | 调度器 | 尽量将 API/Web 副本分布到不同节点 |

配置清单位于：

- `D:\Develop\OpsPilot\deploy\k8s\vm-lab`
- 部署脚本：`D:\Develop\OpsPilot\scripts\k8s\deploy-vm-lab.sh`
- 预检脚本：`D:\Develop\OpsPilot\scripts\k8s\preflight-vm-lab.sh`
- 预检回归测试：`D:\Develop\OpsPilot\scripts\k8s\tests\preflight-vm-lab.test.sh`

---

## 6. 阶段 A：本地构建与自动化测试

所有 Windows 端操作均在 `D:\Develop\OpsPilot` 执行，缓存也放在 D 盘项目或 D 盘开发目录中。

### 6.1 后端测试

```powershell
Set-Location D:\Develop\OpsPilot
mvn -B -ntp "-Dmaven.repo.local=D:\Develop\maven-repository" test
```

验收结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

这里证明的是：当前代码对应的 7 个自动化测试全部通过。它不能单独证明 Kubernetes 部署正确，所以后面仍然进行了镜像、集群和业务验收。

### 6.2 后端打包

```powershell
mvn -B -ntp "-Dmaven.repo.local=D:\Develop\maven-repository" package
```

产物：

```text
D:\Develop\OpsPilot\target\opspilot-0.1.0-SNAPSHOT.jar
```

打包过程再次执行了 7 个测试并成功结束。

构建期间出现了一个兼容性提示：项目使用的 Flyway 11.7.2 提示 MySQL 8.4 高于其最新测试版本 8.1。今天的迁移和测试能够通过，但该提示仍需后续通过依赖升级说明或版本兼容性验证来处理，不能把它当成已经消失的问题。

### 6.3 前端依赖与构建

```powershell
npm --prefix frontend ci
npm --prefix frontend run build
```

验收结果：

- 安装 77 个包。
- Vite 6.4.3 处理 1594 个模块。
- 构建耗时约 7.46 秒。

第一次执行时，受限进程不能在工作区外创建 `D:\Develop\npm-cache`，而且旧的 `node_modules` 清理出现 `EPERM/ENOTEMPTY`。处理方式是将缓存放到项目内部的 `D:\Develop\OpsPilot\.npm-cache`，并重新执行干净安装。

`npm` 同时报告 3 个 high 级别依赖风险。今天没有使用 `npm audit fix --force` 强行升级，因为这种操作可能引入破坏性版本变化。正确后续动作是查看具体依赖链、判断是否影响运行时，再制定升级和回归测试方案。

---

## 7. 阶段 B：构建、导出和校验镜像

### 7.1 镜像结果

| 镜像 | 平台 | 大小 |
| --- | --- | ---: |
| `opspilot-api:0.1.1` | linux/amd64 | 约 123 MB |
| `opspilot-web:0.1.1` | linux/amd64 | 约 26 MB |
| `mysql:8.4` | linux/amd64 | 约 239 MB |

### 7.2 离线镜像包

由于虚拟机实验环境不依赖远程镜像仓库，本次采用“Windows 构建并导出 tar，再上传到两个 worker”的离线方式。

本地产物：

```text
D:\Develop\OpsPilot\dist\k8s-vm-lab\opspilot-images-0.1.1.tar
```

文件证据：

```text
大小：403,514,368 bytes（约 384.82 MiB）
SHA-256：C0B1D8D2E059687BE5D9BD8997822C8FFEB38F02327B11DF3F7783ED79512625
```

校验和的意义：上传前后分别计算 SHA-256。如果值一致，可以证明传输过程中没有发生字节级损坏。它不能证明镜像业务一定正确，因此仍需执行 `docker load`、Pod 启动和业务验收。

---

## 8. 阶段 C：上传并导入两个工作节点

### 8.1 操作位置

| 操作 | node1 `.101` | node2 `.102` | master `.100` |
| --- | --- | --- | --- |
| 上传镜像 tar | 是 | 是 | 否 |
| 校验 tar | 是 | 是 | 否 |
| `docker load` | 是 | 是 | 否 |
| 上传 K8s YAML/部署脚本 | 否 | 否 | 是 |
| 执行 `kubectl` | 否 | 否 | 是 |

两个工作节点使用的目录均为：

```text
/root/opspilot
```

master 使用的部署目录为：

```text
/root/opspilot-deploy
```

### 8.2 工作节点校验与导入

在 node1 和 node2 分别执行：

```bash
cd /root/opspilot
sha256sum opspilot-images-0.1.1.tar
docker load -i opspilot-images-0.1.1.tar
docker images --format '{{.Repository}}:{{.Tag}}' \
  | grep -E '^(opspilot-api:0.1.1|opspilot-web:0.1.1|mysql:8.4)$'
```

两台节点计算得到的校验值均为：

```text
c0b1d8d2e059687be5d9bd8997822c8ffeb38f02327b11df3f7783ed79512625
```

两台节点均确认存在三个标签：

```text
opspilot-api:0.1.1
opspilot-web:0.1.1
mysql:8.4
```

为什么必须在两个 worker 都导入？因为 Pod 可能被调度到任意一个带工作负载标签的 worker。如果只有一个节点有镜像，调度到另一个节点的 Pod 会因无法获取本地镜像而启动失败。

---

## 9. 阶段 D：集群预检与兼容性修复

### 9.1 第一次预检失败

原脚本使用下列方式检查 PDB：

```bash
kubectl api-resources --api-version=policy/v1 | grep PodDisruptionBudget
```

在当前 Kubernetes v1.21.10 环境中出现了误判，脚本报告 API Server 不支持 `policy/v1` PodDisruptionBudget，但集群实际上支持该 API。

### 9.2 根因

问题不在集群，而在预检方法。不同版本 `kubectl api-resources` 的显示行为会产生差异，基于展示文本做精确匹配不够稳定。

### 9.3 修复

脚本改为查询 API Server 的原始发现接口：

```bash
kubectl get --raw /apis/policy/v1
```

然后在 JSON 中检查：

```text
"name":"poddisruptionbudgets"
```

并为 Kubernetes v1.21 的行为增加了模拟 `kubectl` 回归测试，先复现失败，再验证修复后通过。

最终预检输出：

```text
PASS: Kubernetes=v1.21.10, 三节点 Ready, worker 使用 Docker, Calico 与 policy/v1 PDB 可用.
```

这次故障带来的原则：

> 检查 Kubernetes 能力时，优先验证 API discovery 或直接查询资源，不要只依赖面向人的表格输出和字符串匹配。

---

## 10. 阶段 E：存储、节点标签与 Secret

### 10.1 在 node1 `.101` 创建 MySQL 数据目录

```bash
mkdir -p /data/opspilot/mysql
chown -R 999:999 /data/opspilot/mysql
chmod 700 /data/opspilot/mysql
stat -c 'path=%n owner=%u group=%g mode=%a' /data/opspilot/mysql
```

验收结果：

```text
path=/data/opspilot/mysql owner=999 group=999 mode=700
```

其中 `999:999` 对应 MySQL 容器内使用的用户和组。`700` 表示只有所有者可以读、写和进入该目录。

### 10.2 在 master `.100` 标记工作节点

```bash
kubectl label node k8s-node1 opspilot.io/workload=true --overwrite
kubectl label node k8s-node2 opspilot.io/workload=true --overwrite
kubectl get nodes -L opspilot.io/workload
```

验收结果：node1 与 node2 的 `WORKLOAD` 均为 `true`，master 没有该标签。业务 YAML 的节点选择条件因此会把业务 Pod 调度到两个 worker，避免占用控制平面节点。

### 10.3 在 master `.100` 创建 Namespace 和 Secret

Namespace：

```text
opspilot
```

Secret：

```text
opspilot-database
```

字段：

```text
database-password
mysql-root-password
```

密码通过隐藏输入读入 shell 变量，再通过 `--dry-run=client -o yaml | kubectl apply -f -` 创建或更新 Secret，最后立即 `unset` 变量。本文不保存 Secret 的实际值。

注意：Secret 的常规值只是 Base64 编码，并不等同于加密。生产环境还应配置 etcd 静态加密、最小 RBAC 权限，并考虑外部 Secret 管理系统。

---

## 11. 阶段 F：部署 OpsPilot

在 master `.100` 执行：

```bash
cd /root/opspilot-deploy
./deploy-vm-lab.sh /root/opspilot-deploy/vm-lab
```

### 11.1 部署结果

| 对象 | 结果 |
| --- | --- |
| Namespace | `opspilot` Active |
| MySQL | `mysql-0`，1/1 Running，位于 node1 |
| API | 2 个 Pod，分别分布到 node1/node2 |
| Web | 2 个 Pod，分别分布到 node1/node2 |
| MySQL Service | Headless Service，3306/TCP |
| API Service | ClusterIP，18080/TCP |
| Web Service | NodePort，8080:30080/TCP |
| PVC | `opspilot-mysql-data`，Bound，5 Gi，RWO |
| PDB | API 与 Web 各 1 个，已创建 |
| NetworkPolicy | 4 个策略，已创建 |

### 11.2 访问地址

```text
http://192.168.99.101:30080
http://192.168.99.102:30080
```

浏览器显示“不安全”是因为当前使用明文 HTTP NodePort，没有配置 HTTPS 证书。这不代表业务不可用，但说明 Ingress/TLS 仍是后续任务。

### 11.3 健康检查证据

两个入口的 `/health` 均返回：

```text
status=UP
environment=vm-kubernetes-lab
version=0.1.1
```

这证明两个节点入口都能经过 NodePort、Web/Nginx、API Service 到达后端健康接口。

---

## 12. 部署故障：API 首次启动出现数据库通信失败

### 12.1 现象

部署完成后，两个 API Pod 都显示过 `RESTARTS=1`。查看前一次容器日志发现：

```text
com.mysql.cj.exceptions.CJCommunicationsException: Communications link failure
java.io.EOFException: Can not read response from server
```

异常发生在 Flyway 建立 JDBC 连接和执行数据库迁移之前。

### 12.2 调查证据

API 的 initContainer 当前只执行 TCP 端口检查：

```text
until nc -z -w 2 "$DB_HOST" "$DB_PORT"
```

TCP 端口开始监听并不等于 MySQL 已经完成协议握手、系统表初始化、用户创建和认证准备。

### 12.3 根因判断

API 在 MySQL 端口刚打开时就开始启动，Flyway 立即连接数据库；此时 MySQL 尚未完全 Ready，所以第一次连接被中断。Kubernetes 根据重启策略重新启动 API，第二次启动时 MySQL 已经准备好，因此 API 成功运行。

后来在 MySQL 已稳定的情况下新建的 API Pod 启动后 `RESTARTS=0`，与上述判断一致。

### 12.4 当前状态与后续修复

当前业务已恢复，但启动门禁缺陷仍未修复。后续应将“只检查 TCP 端口”改为“执行经过认证的 MySQL 查询”，例如通过专用探针账号执行 `SELECT 1`，并为等待逻辑增加合理超时与错误日志。

不能把这次自动重启解释为“没有问题”。正确表述是：

> Kubernetes 通过容器重启恢复了暂时性启动失败，但我们也发现了应用依赖就绪检查不充分的问题。

---

## 13. 阶段 G：业务闭环验收

### 13.1 跨节点账户验证

通过 `.101:30080` 创建账户，再通过 `.102:30080` 查看账户列表，能够看到相同数据。这说明：

1. 两个 NodePort 入口都能路由到 Web Service。
2. Web 能通过 API Service 访问后端副本。
3. 后端副本连接的是同一个 MySQL。
4. 数据没有保存在某个前端或 API Pod 的临时文件系统中。

今日创建的两条演示账户：

| 账号 | 名称 | 初始余额 |
| --- | --- | ---: |
| `6222026081800001` | 秋招演示付款账户 | ¥10,000.00 |
| `6222026081800002` | 秋招演示收款账户 | ¥2,000.00 |

### 13.2 转账验收

| 项目 | 值 |
| --- | --- |
| 请求编号 | `web-20260818093326-82bjyj` |
| 付款账户 | `6222026081800001` |
| 收款账户 | `6222026081800002` |
| 金额 | ¥1,250.50 |
| 结果 | COMPLETED |
| 付款账户转账后余额 | ¥8,749.50 |
| 收款账户转账后余额 | ¥3,250.50 |

金额守恒检查：

```text
转账前总额 = 10,000.00 + 2,000.00 = 12,000.00
转账后总额 =  8,749.50 + 3,250.50 = 12,000.00
```

双录流水检查：

- 付款账户生成一条 DEBIT 流水。
- 收款账户生成一条 CREDIT 流水。
- 两条流水金额均为 ¥1,250.50。
- 页面显示“借贷平衡”。

### 13.3 幂等性验证

使用相同请求编号重新提交后，系统没有再次扣款、没有新增第二笔转账，也没有多生成借贷流水。

这说明当前转账接口具备基于幂等键防止重复提交的业务能力。其核心含义是：网络重试或用户重复点击时，同一个业务请求只产生一次资金变化。

---

## 14. 前端故障：中文幂等键无法放入 HTTP Header

### 14.1 现象

在“请求编号（幂等键）”输入框中填入中文后，页面显示：

```text
Failed to execute 'fetch' on 'Window': Failed to read the 'headers'
property from 'RequestInit': String contains non ISO-8859-1 code point.
```

### 14.2 故障发生位置

错误由浏览器 Fetch API 在构造请求头时抛出，请求尚未到达 Nginx、Spring Boot 或 MySQL。因此这次失败没有产生转账记录。

### 14.3 根因

页面允许输入任意 Unicode 字符，但该值会被放入 `Idempotency-Key` HTTP Header。浏览器对请求头值有字符约束，中文字符不能直接以当前方式写入该 Header。

### 14.4 临时处理与正式修复方向

临时处理：保留页面自动生成的 ASCII 请求编号，或手动输入英文、数字、连字符。

正式修复：

1. 前端限制幂等键只允许约定的 ASCII 字符集。
2. 在输入框下方明确给出格式说明。
3. 提交前校验并显示中文友好提示。
4. 后端 DTO、接口文档和前端校验使用同一份长度与字符规则。

该问题今天只完成了定位和绕过，尚未修改代码。

---

## 15. 阶段 H：Deployment 自愈实验

### 15.1 实验目标

验证 API 的 2 副本不是静态启动的两个容器，而是由 Deployment/ReplicaSet 持续维持的期望状态。

### 15.2 操作位置

以下命令全部在 master `.100` 执行。

先动态选择 node2 上的 API Pod：

```bash
API_POD=$(kubectl -n opspilot get pod \
  -l app.kubernetes.io/component=api \
  --field-selector spec.nodeName=k8s-node2 \
  -o jsonpath='{.items[0].metadata.name}')

echo "准备删除: $API_POD"
```

删除并观察：

```bash
kubectl -n opspilot delete pod "$API_POD"
kubectl -n opspilot get pods \
  -l app.kubernetes.io/component=api \
  -o wide \
  --watch
```

### 15.3 实验结果

被删除的 Pod：

```text
opspilot-api-699c5d49c9-8x2mz
```

自动创建的新 Pod：

```text
名称：opspilot-api-699c5d49c9-v45hg
节点：k8s-node2
Pod IP：10.244.169.149
状态变化：0/1 Running -> 1/1 Running
达到 Ready：约 46 秒
重启次数：0
```

### 15.4 原理解释

完整控制链路是：

```text
删除 Pod
  -> API Server 中实际副本数变为 1
  -> ReplicaSet Controller 发现实际状态小于期望状态 2
  -> 创建一个新 Pod
  -> Scheduler 将新 Pod 分配到满足条件的节点
  -> kubelet 拉起容器并执行探针
  -> Readiness 成功
  -> Service Endpoints 加入新 Pod IP
```

要点：

- 恢复的不是原 Pod，而是新 Pod，所以名称和 IP 都发生变化。
- Service 使用标签和 Ready 状态寻找后端，客户端不需要知道 Pod IP。
- Readiness 未通过时，新 Pod 不应接收业务流量。
- 这是 Deployment/ReplicaSet 的自愈，不是 PDB 的效果。
- 直接 `kubectl delete pod` 不等于 eviction；PDB 是否有效需要通过 `kubectl drain` 等主动驱逐场景另行验证。

---

## 16. 今日故障与处理总表

| 问题 | 现象 | 根因 | 今日处理 | 状态 |
| --- | --- | --- | --- | --- |
| PDB 预检误报 | 报告不支持 `policy/v1` | 依赖 `api-resources` 展示文本匹配 | 改查 API discovery，并加回归测试 | 已修复并验证 |
| 本地 Docker 受限 | 受限进程无法访问 Docker pipe/config | 工具沙箱权限与用户 PowerShell环境不同 | 在具有 Docker 权限的环境执行构建 | 环境绕过，非项目缺陷 |
| npm 清理/缓存失败 | `EPERM/ENOTEMPTY` | 旧目录占用及缓存路径权限 | 使用项目内 D 盘缓存并干净安装 | 已解决本次构建 |
| Flyway 兼容性提示 | MySQL 8.4 高于最新测试版本 8.1 | 依赖兼容性提示 | 保留告警并列入升级验证 | 待处理 |
| npm 高危依赖提示 | 3 个 high | 前端依赖树存在安全告警 | 未强制升级，等待依赖链评估 | 待处理 |
| API 初次启动重启 | `RESTARTS=1`、JDBC 通信失败 | TCP 可用早于 MySQL 完全就绪 | Kubernetes 重启后恢复，定位门禁缺陷 | 业务恢复，正式修复待做 |
| 中文幂等键失败 | Fetch Header 字符错误 | Unicode 值直接进入 HTTP Header | 使用 ASCII 请求编号完成验收 | 代码修复待做 |

---

## 17. 已验证、仅配置和尚未完成的边界

### 17.1 已经通过实验验证

- 三节点均为 Ready。
- 两个 worker 均拥有所需离线镜像。
- API/Web 双副本可以跨 node1、node2 分布。
- NodePort `.101:30080` 和 `.102:30080` 均可访问。
- 健康检查返回 UP。
- 账户创建、查询、转账和流水业务闭环正确。
- 相同幂等键不会重复扣款。
- 删除一个 API Pod 后，Deployment 自动补齐并恢复 Ready。
- PVC 已 Bound，MySQL 正在使用 node1 本地目录。

### 17.2 已配置，但没有通过故障实验验证

- PDB 对 `drain/eviction` 的保护效果。
- NetworkPolicy 对未授权通信的阻断效果。
- 资源 requests/limits 在资源压力下的行为。
- topology spread 在节点不可用时的调度边界。
- liveness/readiness 在真实应用卡死或依赖故障时的行为。
- local PV 在 MySQL Pod 重建后的数据持久性。

“YAML 中写了”只能说明对象已配置，不能替代实验结果。

### 17.3 尚未完成

- 删除 MySQL Pod 并验证数据持久化。
- Deployment 滚动更新、发布观察和版本回滚。
- `kubectl drain` 与 PDB 实验。
- NetworkPolicy 正反向通信实验。
- OOMKilled、Pending、QoS 和资源限制排障。
- HPA 与 Metrics Server。
- Ingress、域名和 TLS。
- Prometheus、Grafana、Alertmanager、日志聚合和告警闭环。
- 私有镜像仓库与 CI/CD。
- MySQL 备份、恢复演练和定时任务。
- 受支持 Kubernetes 版本 + containerd 的现代化部署。
- 多控制平面和 etcd 高可用。

---

## 18. 当前 Kubernetes 结合深度评估

| 层级 | 说明 | OpsPilot 当前状态 |
| --- | --- | --- |
| L0：容器运行 | 单机 Docker 运行应用 | 已超过 |
| L1：基础编排 | Deployment、Service、ConfigMap、Secret | 已完成 |
| L2：状态与可靠性 | StatefulSet、PV/PVC、探针、双副本、自愈 | 部分完成，基础自愈已验证，存储故障实验待做 |
| L3：运维治理 | PDB、NetworkPolicy、requests/limits、发布回滚 | 已有配置，实验验证不足 |
| L4：弹性与可观测性 | HPA、Ingress、指标、日志、告警 | 尚未部署到该集群 |
| L5：交付与生产化 | Registry、CI/CD、备份恢复、安全治理、HA | 尚未完成 |

结论：项目已经与 Kubernetes 形成了真实部署和业务运行层面的结合，但还没有达到“深度结合”或“生产化”的完整程度。下一阶段的重点不是继续堆 YAML，而是通过故障、发布、监控和恢复实验，把已有配置变成能够解释和复现的证据。

---

## 19. 秋招简历与面试表述边界

### 19.1 当前可以如实描述

在能够独立复现并解释今天流程后，可以表述为：

> 在三节点 Kubernetes 实验集群中完成 Spring Boot、React/Nginx 与 MySQL 的容器化部署，配置 Deployment、StatefulSet、Service、ConfigMap、Secret 和 local PV/PVC；通过双节点 NodePort、真实转账与 API Pod 删除实验，验证跨节点访问、数据一致性和 Deployment 基础自愈。

当前如果仍需要逐步指导，应在口头说明中如实补充“在指导下完成，并正在通过重复实验转化为独立能力”。

### 19.2 当前不应写

- 精通 Kubernetes。
- 具备生产级 Kubernetes 集群运维经验。
- 完成 MySQL 高可用。
- 完成完整 CI/CD、灰度发布或全链路可观测性。
- 完成多控制平面容灾。

### 19.3 面试时必须能回答

1. 为什么 API 和 Web 用 Deployment，而 MySQL 用 StatefulSet？
2. Pod 被删除后，是谁发现副本不足并创建新 Pod？
3. Service 为什么不依赖固定 Pod IP？
4. readiness 与 liveness 分别解决什么问题？
5. `RESTARTS=1` 的证据在哪里，为什么不是简单说“数据库慢”？
6. local PV 为什么不是 MySQL 高可用？node1 宕机时会怎样？
7. PDB 能否阻止 `kubectl delete pod`？为什么？
8. Secret 为什么不等于真正加密？
9. 两个 NodePort 地址为什么能看到相同数据？
10. 如何证明转账既没有丢钱，也没有重复扣钱？

---

## 20. 今日复述模板

可以按以下顺序复述今天的实战：

> 我先在 Windows 的 D 盘项目目录完成后端测试、打包和前端构建，再构建 API、Web、MySQL 三个 linux/amd64 镜像。因为实验集群不依赖镜像仓库，我把镜像导出为 tar，通过 SHA-256 校验后导入 node1 和 node2。随后在 master 上通过 Kustomize 清单部署 Namespace、ConfigMap、Secret、Deployment、StatefulSet、Service、PV/PVC、PDB 和 NetworkPolicy。Web 和 API 各两个副本分布在两个 worker，MySQL 使用 node1 的 local PV。之后我从两个 NodePort 入口完成账户与转账验证，并检查双录流水和金额守恒。最后主动删除 node2 上的 API Pod，观察到 ReplicaSet 自动补齐新 Pod，readiness 通过后重新加入 Service。过程中还定位了 PDB 预检误报、MySQL 未完全就绪导致 API 首次重启，以及中文幂等键无法进入 HTTP Header 三个问题。

如果不能脱离本文复述上述过程，就说明操作已经完成，但知识还没有真正掌握，需要再次独立复现。

---

## 21. 下一次实战起点

建议严格按下列顺序继续，每一步都采用“原理 -> 观察 -> 操作 -> 故障 -> 复述 -> 验收”：

1. **MySQL 持久化实验**：记录账户和流水，删除 `mysql-0`，等待重建，再验证数据仍存在。
2. **修复 API 启动门禁**：将 TCP 检查升级为经过认证的 MySQL 查询，并做失败/成功回归。
3. **滚动更新与回滚**：发布新 API 标签，观察新旧 ReplicaSet，再模拟异常版本并回滚。
4. **PDB 与 drain**：确认主动驱逐受 PDB 约束，同时理解 PDB 不处理哪些故障。
5. **NetworkPolicy 故障实验**：从允许和不允许的 Pod 分别发起连接，验证策略真正生效。
6. **资源与调度实验**：制造 Pending、OOMKilled，观察 requests、limits、QoS 和事件。
7. **现代集群路线**：在受支持的 Kubernetes + containerd 环境部署 Helm Chart，再学习 HPA、Ingress 和 RBAC。
8. **可观测性与发布治理**：接入 Prometheus/Grafana/Alertmanager、日志和 CI/CD。
9. **数据保护**：完成 MySQL 备份、破坏、恢复的完整演练。

下一次开始前，在 master `.100` 先执行基线检查：

```bash
kubectl get nodes -o wide
kubectl -n opspilot get pods -o wide
kubectl -n opspilot get svc
kubectl -n opspilot get pvc,pv
kubectl -n opspilot get events --sort-by=.metadata.creationTimestamp | tail -n 30
```

只有基线正常，才进入 MySQL Pod 删除实验；如果基线异常，应先诊断，不能把已有故障混入新实验。

---

## 22. 常用排障命令附录

以下命令主要在 master `.100` 执行。

### 22.1 查看对象和分布

```bash
kubectl -n opspilot get pods -o wide
kubectl -n opspilot get deploy,rs,statefulset
kubectl -n opspilot get svc,endpoints
kubectl -n opspilot get pv,pvc
kubectl get nodes -L opspilot.io/workload
```

### 22.2 查看为什么没启动

```bash
kubectl -n opspilot describe pod <POD_NAME>
kubectl -n opspilot get events --sort-by=.metadata.creationTimestamp
kubectl -n opspilot logs <POD_NAME>
kubectl -n opspilot logs <POD_NAME> --previous
```

`--previous` 对排查发生过重启的容器非常重要，因为当前容器日志可能已经是成功启动后的内容。

### 22.3 查看滚动发布

```bash
kubectl -n opspilot rollout status deployment/opspilot-api
kubectl -n opspilot rollout history deployment/opspilot-api
kubectl -n opspilot rollout undo deployment/opspilot-api
```

### 22.4 查看 Service 是否选中 Pod

```bash
kubectl -n opspilot get svc opspilot-backend -o yaml
kubectl -n opspilot get endpoints opspilot-backend -o wide
kubectl -n opspilot get pods \
  -l app.kubernetes.io/component=api \
  --show-labels
```

---

## 23. 证据与源码位置

| 内容 | 位置 |
| --- | --- |
| 项目源码 | `D:\Develop\OpsPilot` |
| 虚拟机实验清单 | `D:\Develop\OpsPilot\deploy\k8s\vm-lab` |
| VM Lab 部署说明 | `D:\Develop\OpsPilot\deploy\k8s\vm-lab\README.md` |
| 集群预检脚本 | `D:\Develop\OpsPilot\scripts\k8s\preflight-vm-lab.sh` |
| 部署脚本 | `D:\Develop\OpsPilot\scripts\k8s\deploy-vm-lab.sh` |
| 预检回归测试 | `D:\Develop\OpsPilot\scripts\k8s\tests\preflight-vm-lab.test.sh` |
| 离线镜像包 | `D:\Develop\OpsPilot\dist\k8s-vm-lab\opspilot-images-0.1.1.tar` |
| master 部署目录 | `/root/opspilot-deploy` |
| worker 镜像目录 | `/root/opspilot` |
| MySQL 节点数据目录 | `k8s-node1:/data/opspilot/mysql` |

项目中还存在 Helm Chart：

```text
D:\Develop\OpsPilot\deploy\k8s\helm\opspilot
```

该 Chart 声明需要 Kubernetes `>=1.27.0-0`，并包含 HPA、Ingress、RBAC 等模板。它尚未部署到当前 v1.21 实验集群，应该留到现代 Kubernetes 环境中验证，不能把“仓库里已经有模板”写成“当前集群已经运行这些能力”。

---

## 24. 最终验收结论

今天的实战已经形成了四类可展示证据：

1. **构建证据**：Java 7 个测试通过、后端打包成功、前端构建成功。
2. **部署证据**：三节点 Ready、五个业务 Pod Running、PVC Bound、两个 NodePort 地址可访问。
3. **业务证据**：账户跨入口可见、转账完成、借贷双流水平衡、金额守恒、幂等重试不重复扣款。
4. **运维证据**：识别首次启动异常，读取 previous 日志定位 MySQL 就绪竞态；删除 API Pod 后，新 Pod 自动创建并恢复 Ready。

阶段性判断：

> OpsPilot 已完成 Kubernetes 三节点实战的可运行基线，但距离秋招中能够经得住追问的“系统掌握”仍有明显距离。后续必须继续完成存储恢复、发布回滚、PDB、网络策略、资源故障、监控告警、备份恢复和现代化集群实验，并至少独立复现一次今天的全过程。
