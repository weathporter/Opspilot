# OpsPilot 全景百科与实战学习手册

*从 Linux、网络与 Docker 基础，到 Java/MySQL 业务闭环、可观测性和 Kubernetes 交付*

版本：学习版 v1.0（2026-08-13）
项目位置：`D:\Develop\OpsPilot`
适用对象：已经学过 Linux 基础命令、了解 Docker 基本功能，正在学习计算机网络，需要把一个完整项目真正变成自己能力的校招学习者。


---

## 全书目录

> 目录只列篇和章；章内小节可使用编辑器的大纲面板快速跳转。

- **[第一篇：项目全貌](#part-01)**
  - [第 1 章　OpsPilot 到底是什么](#chapter-01)
  - [第 2 章　完整技术栈与职责地图](#chapter-02)
  - [第 3 章　系统总架构与六条闭环](#chapter-03)
  - [第 4 章　目录结构与模块边界](#chapter-04)
- **[第二篇：先把系统运行起来——Linux 与网络基础](#part-02)**
  - [第 5 章　Linux 不是命令表，而是应用运行的地基](#chapter-05)
  - [第 6 章　systemd：让 Java 成为可管理的系统服务](#chapter-06)
  - [第 7 章　Nginx：统一入口、反向代理与故障分界线](#chapter-07)
  - [第 8 章　从浏览器到 MySQL：网络路径必须逐层说清](#chapter-08)
  - [第 9 章　Linux 第一响应：遇到故障时到底怎样思考](#chapter-09)
- **[第三篇：容器不是魔法——Docker 与 Compose](#part-03)**
  - [第 10 章　Docker 的对象模型：镜像、容器、网络和卷](#chapter-10)
  - [第 11 章　Dockerfile：从源码构建安全、可运行的镜像](#chapter-11)
  - [第 12 章　Docker Compose：单机上的完整系统编排](#chapter-12)
  - [第 13 章　Docker 常见故障百科](#chapter-13)
- **[第四篇：真正的业务核心——Java、Spring Boot 与 MySQL](#part-04)**
  - [第 14 章　Spring Boot 应用怎样启动并连接各层](#chapter-14)
  - [第 15 章　HTTP 分层：Filter、Controller、DTO、Service、Repository](#chapter-15)
  - [第 16 章　REST API 与统一错误契约](#chapter-16)
  - [第 17 章　MySQL 数据模型：账户、订单和流水](#chapter-17)
  - [第 18 章　事务：为什么四类写入要么全成、要么全败](#chapter-18)
  - [第 19 章　转账算法：幂等、悲观锁、死锁与双边流水](#chapter-19)
  - [第 20 章　JPA/Hibernate：项目里实际发生了什么](#chapter-20)
  - [第 21 章　运维总览读模型：真实数据，不是静态大屏](#chapter-21)
  - [第 22 章　后端测试：怎样证明，而不是怎样自信](#chapter-22)
  - [第 23 章　后端核心实验与验收](#chapter-23)
- **[第五篇：用户看见什么——React 前端与真实演示闭环](#part-05)**
  - [第 24 章　前端架构：页面不是静态大屏](#chapter-24)
  - [第 25 章　前端构建、联调与故障排查](#chapter-25)
- **[第六篇：把系统变得可观察——指标、日志与告警](#part-06)**
  - [第 26 章　可观测性到底解决什么问题](#chapter-26)
  - [第 27 章　Actuator 与 Micrometer](#chapter-27)
  - [第 28 章　Prometheus：抓取、时序、PromQL 与规则](#chapter-28)
  - [第 29 章　Grafana：看板怎样成为证据](#chapter-29)
  - [第 30 章　日志链路：Alloy、Loki 与请求 ID](#chapter-30)
  - [第 31 章　Alertmanager：它到底是干什么的](#chapter-31)
  - [第 32 章　可观测性实验与验收](#chapter-32)
- **[第七篇：从单机容器到 Kubernetes](#part-07)**
  - [第 33 章　为什么有 Compose 还要 Kubernetes](#chapter-33)
  - [第 34 章　Pod、Deployment、ReplicaSet：应用怎样自愈](#chapter-34)
  - [第 35 章　Service、EndpointSlice 与 Ingress](#chapter-35)
  - [第 36 章　ConfigMap、Secret 与配置变更](#chapter-36)
  - [第 37 章　StatefulSet、PVC 与 MySQL 边界](#chapter-37)
  - [第 38 章　三类探针与优雅摘流](#chapter-38)
  - [第 39 章　requests、limits 与 HPA](#chapter-39)
  - [第 40 章　PDB、拓扑与计划中断](#chapter-40)
  - [第 41 章　RBAC 与 ServiceAccount](#chapter-41)
  - [第 42 章　NetworkPolicy](#chapter-42)
  - [第 43 章　Helm：参数化安装、升级和回滚](#chapter-43)
  - [第 44 章　Minikube 一键部署脚本逐步拆解](#chapter-44)
  - [第 45 章　Kubernetes 排障百科](#chapter-45)
  - [第 46 章　Kubernetes 实验与个人掌握标准](#chapter-46)
- **[第八篇：交付、运行与企业级演进](#part-08)**
  - [第 47 章　四种运行方式：先知道自己正在运行哪一套](#chapter-47)
  - [第 48 章　完整 Docker Compose 运行手册](#chapter-48)
  - [第 49 章　Rocky Linux 原生部署全流程](#chapter-49)
  - [第 50 章　自动化脚本：把人工步骤变成受控流程](#chapter-50)
  - [第 51 章　发布、健康门禁与回滚](#chapter-51)
  - [第 52 章　备份、恢复、RPO 与 RTO](#chapter-52)
  - [第 53 章　故障演练与事件处理](#chapter-53)
  - [第 54 章　安全：当前项目做了什么，还缺什么](#chapter-54)
  - [第 55 章　企业级差距与合理演进路线](#chapter-55)
  - [第 56 章　OpsPilot 的完整闭环总结](#chapter-56)
- **[第九篇：把项目真正学成自己的能力](#part-09)**
  - [第 57 章　从你当前基础出发的学习地图](#chapter-57)
  - [第 58 章　统一学习法：原理—观察—操作—故障—复述—验收](#chapter-58)
  - [第 59 章　24 个循序渐进实训任务](#chapter-59)
  - [第 60 章　20 分钟项目演示脚本](#chapter-60)
  - [第 61 章　三种时长的项目讲述](#chapter-61)
  - [第 62 章　项目面试追问题库（基础到进阶）](#chapter-62)
  - [第 63 章　简历表述、AI 参与和证据等级](#chapter-63)
- **[第十篇：故障排查百科与值班参考](#part-10)**
  - [第 64 章　证据驱动的排障方法](#chapter-64)
  - [第 65 章　启动与入口故障](#chapter-65)
  - [第 66 章　网络、Nginx 与 HTTP 故障](#chapter-66)
  - [第 67 章　Spring Boot、MySQL 与业务一致性故障](#chapter-67)
  - [第 68 章　Docker 与 Compose 故障](#chapter-68)
  - [第 69 章　Prometheus、Grafana、Loki 与告警故障](#chapter-69)
  - [第 70 章　Kubernetes 故障](#chapter-70)
  - [第 71 章　命令不是答案：按问题查工具](#chapter-71)
  - [第 72 章　核心术语词典](#chapter-72)
  - [第 73 章　最终验收清单](#chapter-73)
  - [第 74 章　资料索引与继续学习](#chapter-74)


# 使用说明：这不是项目说明书，而是你的个人项目百科

这本手册只围绕一个真实项目：OpsPilot。它不会把 Java、Linux、网络、Docker、MySQL、监控和 Kubernetes 拆成互不相干的名词，而是用同一条业务链把它们串起来：浏览器发起请求，Nginx 转发，Spring Boot 执行业务，MySQL 保存结果，Prometheus 和 Loki留下运行证据，出现异常后再通过告警、日志、命令、回滚和恢复完成闭环。

你当前最大的风险并不是“技术不够多”，而是工程已经存在、你却不能独立解释和操作。于是本手册把学习目标分为四级，并故意采用不同深度。

| 深度 | 你要达到的状态 | 本书中的内容 | 当前优先级 |
| --- | --- | --- | --- |
| A：必须掌握 | 不看答案能讲清原理，能亲手执行并排错 | Linux、网络请求链、Docker 对象与网络、Java 分层、MySQL 表与事务、转账流程 | 最高 |
| B：演示前掌握 | 能在项目中定位配置，能完成常见操作和故障恢复 | Nginx、systemd、Shell、Compose、备份恢复、指标日志告警、测试 | 高 |
| C：逐步掌握 | 能解释对象关系，能按脚本部署、观察和回滚 | Kubernetes、Helm、探针、HPA、PDB、RBAC、NetworkPolicy | 中 |
| D：知道边界 | 知道为什么需要、何时引入、当前项目为什么没做 | 生产高可用、TLS/WAF、统一认证、CI/CD、GitOps、OTel、异地灾备 | 低，秋招前逐项补 |

> [!IMPORTANT] 工程实现不等于个人掌握。简历上只能把你亲自运行、观察、故障演练并能解释的能力写成“熟悉”或“熟练”。其余内容应写成“了解”“参与实现”或“完成过本地实践”。

## 每节的学习方法

核心章节尽量遵循同一套闭环：

1. 原理：它解决什么问题，为什么存在。
2. 关系：它在 OpsPilot 中与谁相连，输入和输出是什么。
3. 源码落点：到哪个目录、哪个配置或哪个类中找到证据。
4. 观察：正常时应该看到什么，不能只记命令。
5. 操作：亲手执行最小实验。
6. 故障：故意让它失败，比较正常与异常证据。
7. 复述：用自己的话讲给面试官听。
8. 验收：不看文档完成任务，才能算掌握。

## 五种状态标记

为了避免把“文件写了”说成“线上跑了”，本书始终区分以下状态：

| 标记 | 含义 | 举例 |
| --- | --- | --- |
| 已实现 | 仓库中存在代码、脚本或配置 | Helm Chart 已包含 HPA 模板 |
| 历史已验收 | 2026-08-11 的验收记录证明当时跑通过 | Compose 十个容器曾全部启动；K8s API/Web/MySQL Pod 曾 Ready |
| 当前未实时确认 | 本次编写手册时没有拿到当前运行证据 | Docker Desktop 当前未能从受限会话读取，不能说此刻在线 |
| 需要你亲测 | 代码存在，但必须由你亲手完成才能成为个人能力 | Rocky Linux 原生部署、备份后恢复到隔离库 |
| 生产仍缺失 | 本地学习方案不能等同生产能力 | 内置单副本 MySQL 不是高可用；Alertmanager 尚未接外部通知 |

<a id="part-01"></a>

# 第一篇：项目全貌

<a id="chapter-01"></a>

## 第 1 章　OpsPilot 到底是什么

### 1.1 一句话定义

OpsPilot 是一个以账户、转账、双边资金流水为业务载体，把前端操作、Java 后端、MySQL 持久化、Linux/Docker 部署、监控告警、日志检索、发布回滚、备份恢复和 Kubernetes 交付串成完整闭环的个人项目。

它不是银行核心系统，也不是已经服务真实用户的企业生产系统。它的价值在于：用一个足够真实、但个人可以理解和运行的业务，把开发岗与运维岗共同关注的能力放在同一套证据里。

### 1.2 它解决的业务问题

项目的业务故事很简单：创建两个账户，从付款账户向收款账户转一笔钱，并能够证明这笔钱只扣了一次、余额没有错、订单有记录、借贷流水平衡、失败不会留下半成品数据。

简单不等于随便。恰恰因为“转账”涉及资金一致性，面试官可以继续追问：

- 用户超时后重复提交怎么办？
- 两个请求同时扣同一个账户怎么办？
- A 向 B 转账和 B 向 A 转账会不会死锁？
- 扣款成功、流水写入失败怎么办？
- 数据库重启、应用重启后幂等是否还有效？
- 页面显示成功，怎样从数据库和日志证明？
- 新版本启动失败怎样阻止继续暴露？

OpsPilot 的核心意义就是让这些问题都有代码、配置、测试和运行手册作为答案。

### 1.3 它为什么适合你的目标岗位

对于央国企网络运维、系统运维、云平台运维，项目证明你不是只会背 Linux 命令，而是能围绕一个系统处理端口、进程、反向代理、数据库、监控、告警、发布、备份和故障。

对于私企运维和运维开发，项目证明你能读代码、写脚本、构建镜像、编排服务、理解 HTTP 请求链，并用自动化门禁减少人工误操作。

对于银行科技岗，项目证明你理解 REST API、参数校验、事务、锁、幂等、唯一约束、双边流水和审计证据，而不是只做了一个普通增删改查页面。

### 1.4 项目不是什么

- 不是微服务项目。当前采用模块化单体，一个 Spring Boot 进程、一套 MySQL 数据库。
- 不是大数据或 AI 平台。没有业务需要 Milvus、向量数据库或大模型在线推理。
- 不是生产级银行支付系统。没有真实清算、账务日切、监管报送、密钥机、双活或多地容灾。
- 不是完整安全平台。尚未实现登录认证、角色授权、TLS、WAF、限流和安全审计中心。
- 不是已经跑在多节点生产 Kubernetes 上的系统。Minikube 是单节点实验集群。

> [!NOTE] 简历项目最怕两件事：一是只有 CRUD，没有部署和排障；二是把所有热门组件塞进去，却说不出业务必要性。OpsPilot 选择“业务闭环 + 运维闭环 + 少量进阶能力”，高级组件只有在解决真实问题时才引入。

<a id="chapter-02"></a>

## 第 2 章　完整技术栈与职责地图

### 2.1 开发与数据

| 技术 | 在项目中的职责 | 你当前要掌握到什么程度 |
| --- | --- | --- |
| Java 17 | 实现领域对象、服务编排、异常处理 | 能读懂并独立改一个接口，能调试调用链 |
| Spring Boot 3.5 | 自动配置、Web 容器、配置管理、Actuator | 知道应用如何启动、端口从哪里来、Bean 如何协作 |
| Spring MVC | 把 HTTP 请求映射到 Controller | 能解释 URL、方法、请求头、JSON、状态码 |
| Bean Validation | 在入口拒绝非法账号、金额、空字段 | 能说明为什么校验放在 DTO 边界 |
| Spring Data JPA / Hibernate | 实体映射、仓储查询、脏检查、事务集成 | 会跟踪一次查询和更新，不要求现在精通 Hibernate 内部 |
| MySQL 8.4 | 保存账户、订单、流水和约束 | 必须理解表、索引、事务、锁、唯一约束和备份恢复 |
| Flyway | 版本化管理数据库结构 | 必须知道 V1 执行后不能改，变更使用 V2+ |
| Maven | 依赖、测试、打包 JAR | 会看 pom、执行测试与打包、识别构建失败 |

### 2.2 前端

| 技术 | 作用 | 掌握目标 |
| --- | --- | --- |
| React 18 | 页面和组件状态 | 能说明页面怎样请求真实 API，能改字段或交互 |
| TypeScript | 约束前后端数据结构 | 会读 interface，知道它不能替代服务端校验 |
| Vite | 开发服务器与生产构建 | 会区分开发代理和生产 Nginx |
| React Router | URL 与页面路由 | 知道刷新子路径为什么需要 Nginx 回退 index.html |
| 原生 SVG / CSS | 趋势图与响应式界面 | 能解释数据来自哪里，不要求成为前端专家 |

### 2.3 Linux、容器与交付

| 技术 | 作用 | 掌握目标 |
| --- | --- | --- |
| Rocky Linux 9 | 原生部署与排障环境 | 重点学习用户、权限、进程、端口、服务、日志、资源 |
| systemd | 管理 Java 进程、重启策略和开机启动 | 必须能看 status/journal，理解退出码和重启条件 |
| Nginx | 外部入口、静态资源、反向代理、管理面隔离 | 必须会区分 502/504/404，并会验证 upstream |
| Bash / PowerShell | 构建、发布、备份、巡检、故障演练 | 先能读、能安全运行，再逐步独立写 |
| Docker | 镜像、容器、网络、卷、健康检查 | 必须分清镜像与容器、宿主端口与容器端口 |
| Docker Compose | 单机多服务编排 | 会启动、查看状态、日志、网络和卷，知道 down -v 风险 |
| Kubernetes | 多副本、服务发现、自愈、资源和权限治理 | 先掌握对象关系，再掌握脚本和排障 |
| Helm | 参数化生成和管理 K8s Release | 会读 values/template、安装、升级、历史和回滚 |

### 2.4 可观测性

| 组件 | 它做什么 | 它不做什么 |
| --- | --- | --- |
| Actuator | 暴露健康、信息、指标入口 | 不是接口文档，也不自动保存历史数据 |
| Micrometer | 在 Java 中采集并统一表示指标 | 不是独立监控服务器 |
| Prometheus | 定时抓取指标、保存时序数据、计算告警规则 | 不负责展示完整业务页面，不负责发送最终通知 |
| Grafana | 查询 Prometheus/Loki 并展示看板 | 看板变绿不代表所有业务都正确 |
| Alertmanager | 接收告警，分组、去重、静默、路由 | 不主动抓指标，不判断接口逻辑是否正确 |
| Alloy | 发现 Docker 容器并读取 stdout/stderr | 不负责长期保存日志 |
| Loki | 按标签存储并查询日志 | 不是关系数据库，也不适合高基数标签滥用 |
| Node Exporter | 采集 Linux 主机指标 | Docker Desktop 下看到的主要是 Linux VM，不是完整 Windows 主机 |
| cAdvisor | 采集每个容器的资源使用 | 高权限运行需要理解安全边界 |

<a id="chapter-03"></a>

## 第 3 章　系统总架构与六条闭环

### 3.1 总体架构

```text
用户浏览器
    |
    | HTTP :18000（Compose）或 Ingress/Service（K8s）
    v
React 静态页面 + Nginx
    |  /api/* 反向代理，携带 X-Request-ID
    v
Spring Boot :18080
    |  Controller -> ApplicationService -> Repository
    v
MySQL :3306
    |  account / transfer_order / ledger_entry
    |
    +--> Actuator + Micrometer --> Prometheus --> Alertmanager
    |
    +--> stdout/stderr --> Alloy --> Loki --> Grafana

Linux / Docker / Kubernetes 为上述进程提供运行、网络、资源、发布和恢复环境。
```

### 3.2 业务闭环

开户 -> 回读账户 -> 发起转账 -> 相同幂等键重放 -> 查询订单 -> 查询两条流水 -> 服务端校验借贷平衡。任何一个环节都使用真实 API 和 MySQL，不用前端静态数组冒充。

### 3.3 数据一致性闭环

一次成功转账在同一 MySQL 事务中完成四类写操作：付款方余额减少、收款方余额增加、转账订单新增、两条资金流水新增。任一步抛出运行时异常，事务整体回滚。

### 3.4 部署闭环

源码 -> Maven/TypeScript 构建 -> JAR 和静态资源 -> Docker 镜像或 Linux 版本目录 -> 健康门禁 -> 对外服务。新版本不健康时，Linux 脚本切回旧软链接，Helm 通过 Release 历史回滚。

### 3.5 可观测闭环

请求产生指标与日志 -> Prometheus 抓指标、Alloy 采日志 -> Grafana 展示 -> 规则满足持续时间后 Prometheus 产生告警 -> Alertmanager 分组/静默/路由 -> 操作者根据告警名、时间、版本和请求 ID 查日志与指标。

### 3.6 故障处置闭环

发现 -> 记录影响和时间 -> 保存现场 -> 从入口向内排查 -> 形成证据 -> 修复或回滚 -> 健康检查 -> 业务与数据校验 -> 复盘与预防。重启只是可能的恢复动作，不是排障方法的全部。

### 3.7 学习闭环

原理 -> 观察正常状态 -> 亲手操作 -> 制造小故障 -> 根据证据修复 -> 不看文档复述 -> 通过验收题。项目只有经过这条学习链，才真正属于你。

### 3.8 招聘证据闭环

面试中每个说法都必须能落到一种证据：源码行、配置文件、测试结果、运行状态、指标截图、日志查询、数据库记录、发布历史、备份文件或故障复盘。不能只说“用过 Kubernetes”“搭了 Prometheus”。

<a id="chapter-04"></a>

## 第 4 章　目录结构与模块边界

### 4.1 根目录地图

```text
D:\Develop\OpsPilot
├─ src/                 Java 后端、配置、Flyway、测试
├─ frontend/            React/TypeScript 前端与 Edge 端到端测试
├─ deploy/docker/       容器 Nginx 配置
├─ deploy/linux/        systemd、Nginx、环境文件、logrotate
├─ deploy/k8s/          Helm Chart、生产覆盖示例、K8s 说明
├─ observability/       Prometheus、Grafana、Alertmanager、Loki、Alloy
├─ scripts/             构建、发布、回滚、备份、恢复、巡检、故障注入
├─ docs/                架构、Runbook、验收、排障、学习与面试材料
├─ compose*.yml         Compose 基础栈、观测栈和 Docker Desktop 覆盖
├─ Dockerfile*          后端完整构建与本地运行时构建
├─ pom.xml              Maven 项目与依赖
└─ README.md            项目入口说明
```

### 4.2 Java 业务模块

`account` 负责账户创建、查询、余额与状态规则；`transfer` 负责转账编排、订单、幂等和双边流水；`dashboard` 负责只读聚合；`common` 负责统一异常；`config` 负责时间源等技术配置；`observability` 负责请求关联 ID。

模块边界的目的不是让目录显得复杂，而是防止职责混乱。例如 Controller 不应该直接写 SQL，实体不应该知道 Docker，发布脚本不应该修改业务余额。

### 4.3 为什么是模块化单体

当前业务量、团队规模和资源预算都不需要微服务。模块化单体仍然能清楚分层，同时保留一个进程、一个部署单元和本地数据库事务，学习和排障路径更短。

只有出现以下真实条件时才考虑拆分：某模块需要独立扩缩容、独立发布、独立数据所有权、不同团队维护，或有明显不同的可用性目标。为了简历“看起来高级”而拆服务，只会引入服务发现、分布式事务、链路追踪和网络故障，却没有业务收益。

### 4.4 当前仓库的版本管理风险

本次检查发现 Git 主分支尚无正式提交，文件处于已暂存、已修改和未跟踪混合状态。这不会影响代码能否运行，但会削弱版本审计、发布追溯和协作可信度。

你应在确认敏感文件未提交、项目可构建后建立首个基线提交；之后按“功能/配置/文档/故障修复”形成小而清晰的提交。不要为了好看伪造历史，也不要在不了解工作区内容时执行强制重置。

### 4.5 本章复述模板

“OpsPilot 采用模块化单体。账户、转账和运维总览按业务职责分包，一个 Spring Boot 进程和一个 MySQL 数据库保证学习阶段的事务一致性与排障可控。工程目录把业务代码、Linux、Docker、Kubernetes、观测配置和 Runbook 分开。未来只有出现独立扩容、独立发布或团队边界时才拆服务。”

### 4.6 本章验收

- 不看目录，说出根目录八个主要区域及职责。
- 从 `POST /api/v1/accounts` 依次找到 Controller、DTO、Service、Repository、Entity 和迁移表。
- 解释为什么现阶段只让 Redis 承担共享会话和短 TTL 总览缓存，同时不用微服务、Kafka、Milvus。
- 解释“Git 没有提交”和“项目不能运行”为什么不是同一件事。

<a id="part-01"></a>

# 第二篇：先把系统运行起来——Linux 与网络基础

<a id="chapter-05"></a>

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

<a id="chapter-06"></a>

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

<a id="chapter-07"></a>

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

<a id="chapter-08"></a>

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

<a id="chapter-09"></a>

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

<a id="part-01"></a>

# 第三篇：容器不是魔法——Docker 与 Compose

<a id="chapter-10"></a>

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

<a id="chapter-11"></a>

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

<a id="chapter-12"></a>

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

<a id="chapter-13"></a>

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

<a id="part-01"></a>

# 第四篇：真正的业务核心——Java、Spring Boot 与 MySQL

<a id="chapter-14"></a>

## 第 14 章　Spring Boot 应用怎样启动并连接各层

### 14.1 从 main 方法开始

`OpsPilotApplication` 是 Java 进程入口。`SpringApplication.run` 创建应用上下文，扫描组件，完成自动配置，启动内嵌 Tomcat，并把 Controller、Service、Repository、Filter 等 Bean 连接起来。

启动日志通常经历：读取配置 -> 初始化数据源 -> Flyway 校验/迁移 -> JPA 校验实体映射 -> Tomcat 监听端口 -> 应用可用状态变化。启动失败时应找最早的根因异常，最后一行往往只是外层包装。

### 14.2 Maven 依赖各自解决什么

- `spring-boot-starter-web`：Spring MVC、JSON、嵌入式 Tomcat。
- `spring-boot-starter-validation`：请求字段校验。
- `spring-boot-starter-data-jpa`：实体、仓储、Hibernate 与事务集成。
- `spring-boot-starter-actuator`：健康、信息和指标端点。
- `micrometer-registry-prometheus`：把 Micrometer 指标导出为 Prometheus 格式。
- `flyway-core/flyway-mysql`：数据库结构迁移。
- `mysql-connector-j`：JDBC 驱动。
- 测试依赖：JUnit、MockMvc、Spring Test、Testcontainers MySQL。

Starter 是一组协调版本的依赖集合，不是某个神秘框架。出现冲突时使用 `mvn dependency:tree` 观察实际解析版本。

### 14.3 配置优先级与环境变量

`application.yml` 使用 `${ENV_NAME:default}`。IDE、本地 JAR、Compose、systemd、Kubernetes 可以运行同一个制品，只通过外部配置改变数据库地址、端口、环境和版本。

关键配置：

- 默认应用端口 18080，避开常见 8080。
- `DB_URL` 默认本机 3306；Compose 覆盖为 `mysql:3306`。
- `open-in-view=false`，查询在服务事务内完成，避免 Web 层隐式懒加载。
- `ddl-auto=validate`，Hibernate 只验证，不负责改表。
- Flyway 是结构演进的唯一事实来源。
- 只暴露 health、info、prometheus 三类 Actuator 端点。
- 优雅停机与请求 ID 日志格式启用。

默认开发密码只是方便本地学习，不能作为生产秘密管理方案。

### 14.4 依赖注入

Controller 构造器接收 Service，Service 构造器接收 Repository 和 Clock。Spring 容器在启动时创建并注入依赖。

构造器注入的优点：依赖明确、字段可保持不可变、测试中容易替换、对象创建后立即完整。它不是“为了少写 new”，而是把对象装配交给容器，同时保留类本身的职责边界。

### 14.5 Clock 为什么也要注入

直接在业务各处调用系统当前时间，会让测试结果随运行时刻变化。项目注入 `Clock`，测试可固定时间；写入前还截断到微秒，以匹配 MySQL `TIMESTAMP(6)`，避免 Java 纳秒值与数据库回读值不相等。

这体现一个通用原则：时间、随机数、外部网络等不稳定输入应有可替换边界。

### 14.6 启动失败排查

1. Java 是否为 17。
2. 环境变量是否被当前启动方式加载。
3. 端口是否被占用。
4. JDBC 地址、DNS、3306、用户名密码和权限。
5. Flyway history 与迁移校验和。
6. 实体与表结构是否匹配。
7. 只读文件系统和日志目录权限。
8. 内存和进程退出原因。

<a id="chapter-15"></a>

## 第 15 章　HTTP 分层：Filter、Controller、DTO、Service、Repository

### 15.1 一次请求的完整 Java 路径

以创建账户为例：

```text
HTTP POST /api/v1/accounts
 -> CorrelationIdFilter
 -> AccountController.create
 -> CreateAccountRequest + @Valid
 -> AccountApplicationService.create + @Transactional
 -> Account.open
 -> AccountRepository.saveAndFlush
 -> Hibernate/JDBC
 -> MySQL account
 -> AccountResponse
 -> JSON + HTTP 201
```

每层都只承担自己的职责。你必须能在 IDE 中逐步打断点，而不是只记这个箭头图。

### 15.2 Filter：请求关联 ID

`CorrelationIdFilter` 最先处理请求。若上游 `X-Request-ID` 符合 1 至 64 位安全字符规则，就继续使用；否则生成 UUID。它把 ID 写入 MDC 和响应头，日志格式再从 MDC 读取。

为什么过滤用户输入？如果允许换行等字符直接进入日志，攻击者可能伪造多行记录。为什么 finally 中清理 MDC？Tomcat 会复用线程，不清理会让下一个请求继承错误 traceId。

这不是分布式链路追踪。它只能在当前 HTTP 请求和代理传递范围内关联日志，没有 span、跨异步消息上下文和采样模型。未来引入 OpenTelemetry 后可把它与标准 trace/span 体系衔接。

### 15.3 Controller：协议适配器

Controller 负责 URL、HTTP 方法、请求头、JSON、参数绑定和状态码。它不直接访问 Repository，也不写余额规则。

这样设计的原因：HTTP 只是业务的一种入口。如果规则散落在 Controller，批处理、消息消费或测试直接调用服务时就可能绕过规则。

### 15.4 DTO 与实体不能混用

请求 DTO 表达外部输入契约；响应 DTO 表达对外输出；实体表达持久化状态和领域行为。

直接返回 JPA 实体可能暴露内部主键、版本字段、延迟加载关系，并让数据库模型绑死 API。OpsPilot 的 `AccountResponse`、`TransferResponse` 等在事务内把实体转换为只读快照。

### 15.5 Bean Validation

开户账号要求 8 至 32 位数字，姓名非空且最多 64 字符，余额非负且最多两位小数；转账金额至少 0.01，付款和收款账号格式合法。

前端的 `required`、`min`、`pattern` 只改善用户体验，不能保护服务端，因为调用者可以绕过网页直接发 HTTP。后端校验是可信边界；数据库 CHECK/UNIQUE 又构成最终纵深防线。

### 15.6 Service：用例编排与事务边界

ApplicationService 组合实体、仓储、时间和异常，定义一次用例的事务边界。`@Transactional(readOnly=true)` 表达只读意图，减少不必要的脏检查，但它不是数据库绝对只读安全策略。

事务注解通常依赖 Spring 代理。类内部自调用、非代理对象或异常被吞掉时可能不按预期生效；学习阶段需要知道边界，不必现在深入所有代理实现。

### 15.7 Repository：持久化访问边界

Repository 继承 `JpaRepository`，Spring Data 根据方法名或 JPQL 生成实现。分页参数下推到数据库，避免把全表读进 JVM 后截断。

仓储不是“数据库表的 Controller”。它只负责查询和持久化，不承担 HTTP 或部署职责。

<a id="chapter-16"></a>

## 第 16 章　REST API 与统一错误契约

### 16.1 现有接口清单

| 方法与路径 | 功能 | 成功结果 |
| --- | --- | --- |
| `POST /api/v1/accounts` | 创建账户 | 201 + 账户快照 |
| `GET /api/v1/accounts` | 搜索/列出最近账户 | 200 + 数组 |
| `GET /api/v1/accounts/{accountNo}` | 查询单账户 | 200 或 404 |
| `POST /api/v1/transfers` | 发起/重放转账 | 201 + 订单快照 |
| `GET /api/v1/transfers` | 最近订单和状态筛选 | 200 + 数组 |
| `GET /api/v1/transfers/{requestId}` | 按幂等键查最终状态 | 200 或 404 |
| `GET /api/v1/transfers/{requestId}/ledger` | 订单、两条流水和平衡结果 | 200 或 404 |
| `GET /api/v1/operations/summary` | 运维总览聚合 | 200 + 汇总与趋势 |
| `GET /actuator/health/readiness` | 就绪状态 | 200/非就绪状态 |

当前没有 OpenAPI/Swagger 依赖，Prometheus 也不是接口文档。若要面向协作团队，可补 Springdoc/OpenAPI，但在补之前不能告诉面试官“已提供 Swagger 文档”。

### 16.2 状态码语义

- 400：JSON/字段校验失败。
- 404：账户或订单不存在。
- 409：账号唯一冲突，或同一幂等键被不同载荷复用。
- 422：格式正确但违反业务规则，如余额不足、同账户转账、幂等键为空。
- 500：未预期服务端错误；当前统一处理器没有自定义兜底处理，Spring 会处理。

把所有失败都返回 200 再在 JSON 中放错误码，会破坏 HTTP 基础语义；把业务冲突都当 500，又会污染错误率监控。

### 16.3 ProblemDetail

统一异常处理器把业务异常转换为标准化问题响应，包含 HTTP 状态、稳定 `code`、人类可读 `detail`、时间和 traceId。稳定错误码给程序判断，中文详情给用户和运维理解。

不要把堆栈、SQL、密码或内部路径直接返回客户端。详细异常留在服务端日志，通过 traceId 关联。

### 16.4 限制无界查询

账户和订单列表将 limit 收口在 1 至 100。即使前端只请求 100，服务端也必须保护自己，防止其他调用方传入巨量数字拖慢数据库和网络。

当前使用“最新前 N 条”，未实现完整分页游标。数据规模增长后应增加稳定排序键、分页响应和必要索引，而不是只把上限改大。

<a id="chapter-17"></a>

## 第 17 章　MySQL 数据模型：账户、订单和流水

### 17.1 三张表解决三个不同问题

`account` 保存账户当前状态；`transfer_order` 保存一次转账业务请求和幂等记录；`ledger_entry` 保存余额为什么变化的审计证据。

只保存当前余额无法回答历史变化；只保存流水每次计算当前余额成本高且并发设计更复杂。当前模型用账户快照 + 不可变流水兼顾读取与审计。

### 17.2 account 表

关键字段：

- `id`：内部自增主键。
- `account_no`：外部业务账号，唯一。
- `holder_name`：账户名称。
- `balance DECIMAL(19,2)`：精确十进制余额。
- `status`：ACTIVE/FROZEN/CLOSED 字符串。
- `version`：JPA 乐观锁版本。
- 创建和更新时间：`TIMESTAMP(6)`。

数据库约束：账号唯一、余额不得小于零。

为什么不用 double 存金额？二进制浮点不能精确表示很多十进制小数，累积计算可能产生误差。Java 使用 BigDecimal，MySQL 使用 DECIMAL，并用 compareTo 比较数值而非受 scale 影响的 equals。

### 17.3 transfer_order 表

`request_id` 来自 `Idempotency-Key` 并唯一；同时保存付款账号、收款账号、金额、状态和时间。保存原始业务载荷的目的，是在重试时判断“同一键同一请求”还是“同一键被错误地用于另一请求”。

当前状态只有 PROCESSING 和 COMPLETED。因为订单创建、余额和流水处于同一事务，失败会整体回滚，不留下 FAILED 订单。生产系统若需要记录失败原因和全生命周期，通常会把请求接收、业务执行、失败审计设计成更复杂的状态机或独立审计渠道。

### 17.4 ledger_entry 表

一笔成功转账写两行：付款方 DEBIT、收款方 CREDIT。金额始终为正，方向由 entry_type 表达；`balance_after` 保存交易后余额；外键关联订单。

审计接口根据方向反推交易前余额：DEBIT 的 before = after + amount；CREDIT 的 before = after - amount。

`balanced=true` 的当前判断是正好两条流水且借贷金额相等。它证明这笔订单的双边记录在本模型中平衡，不等于完成全库总账、外部渠道对账或会计科目级审计。

### 17.5 主键、业务键、唯一约束

自增 id 便于内部关联；account_no/request_id 是业务标识。应用层先查是否存在不能消除并发竞态：两个请求可同时查到“不存在”再插入。数据库唯一约束是最终防线。

开户使用 `saveAndFlush` 立即发送 INSERT，让唯一冲突在当前 try/catch 内发生并转换为 409，而不是延迟到事务提交后难以在局部处理。

### 17.6 索引与最左前缀

V1 建立：

- `(account_no, created_at)` 支持按账户和时间查流水。
- `(source_account_no, created_at)` 支持按付款账户和时间查订单。
- 唯一约束通常也带来唯一索引。

复合索引按从左到右的前缀使用。索引不是越多越好：会占空间，INSERT/UPDATE 时维护，统计信息和选择性也影响是否采用。应由真实查询、执行计划和数据规模决定。

当前前端主要按最近时间和 request_id 查询，未来需要结合慢查询与 `EXPLAIN` 评估是否增加 created_at 等索引。不能只根据字段“看起来常用”随意加。

### 17.7 Flyway 迁移

应用启动时 Flyway 读取 `flyway_schema_history`，校验已执行版本的校验和，再按顺序执行新脚本。Hibernate 设置 `ddl-auto=validate`，不会偷偷改表。

已经在持久化/共享环境执行过的 V1 永远不再修改，包括只加注释，因为校验和会变化。任何结构变更创建 V2、V3……并考虑向前/向后兼容、数据回填和回滚策略。

常见 `Migration checksum mismatch` 处理原则：先确认文件是否被错误修改、环境是否执行过、是否存在恶意/意外漂移。不要未经分析就 repair；repair 会更新历史认定，可能掩盖真正的结构差异。

### 17.8 直接查看数据库

Compose 中可进入 MySQL 或从主机 3307 连接：

```sql
USE opspilot;
SHOW TABLES;
DESCRIBE account;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
SELECT account_no, holder_name, balance, status, version FROM account;
SELECT request_id, source_account_no, target_account_no, amount, status FROM transfer_order;
SELECT transfer_order_id, account_no, entry_type, amount, balance_after FROM ledger_entry;
```

不要在没有 WHERE、备份和确认的情况下对共享库执行 UPDATE/DELETE。学习查询使用应用账号；结构/管理操作使用受控管理账号，不让应用使用 root。

<a id="chapter-18"></a>

## 第 18 章　事务：为什么四类写入要么全成、要么全败

### 18.1 事务的直观意义

转账不是四个互不相关的 SQL。业务要求在一个一致性边界内完成：扣款、入账、订单、两条流水。若扣款提交后入账失败，资金凭空减少；若余额变化但流水缺失，审计无法解释。

`@Transactional` 让这些数据库操作使用同一事务。方法正常结束时提交，未捕获运行时异常时回滚。

### 18.2 ACID 用 OpsPilot 解释

- 原子性 Atomicity：四类写入作为整体提交或回滚。
- 一致性 Consistency：约束和业务规则使提交前后满足余额非负、订单唯一、流水有关联等条件。数据库不会自动理解所有业务一致性，应用也要负责。
- 隔离性 Isolation：并发事务不会任意看见彼此未提交中间状态；具体行为由隔离级别和锁决定。
- 持久性 Durability：提交后由 MySQL 日志和存储机制保证重启后仍存在，但这不等于零丢失灾备，仍受刷盘、复制和硬件策略影响。

### 18.3 回滚测试

测试创建余额 100 的付款账户，尝试转 200。实体 `debit` 抛出 `BusinessRuleException`，整个事务回滚。测试断言：两个账户余额不变、订单数为 0、流水数为 0。

这比只断言“抛出了异常”更强，因为它验证失败没有留下数据副作用。

### 18.4 异常与回滚边界

Spring 默认对未检查异常回滚。若捕获异常后不再抛出，外层事务可能仍提交；若抛检查异常而未配置，也可能不按初学者预期回滚。OpsPilot 的业务异常继承运行时异常，保持统一回滚语义。

事务注解经代理生效：同一类内部方法直接调用、私有方法、手工 new 出来的对象都可能绕过代理。面试不必背所有细节，但要知道“写了注解”还需要正确调用边界。

### 18.5 隔离级别与锁的关系

MySQL InnoDB 默认常见隔离级别为 REPEATABLE READ，但实际环境可配置。隔离级别解决读现象，账户扣款仍需要明确的并发更新策略。项目使用 `SELECT ... FOR UPDATE` 悲观写锁，在事务结束前锁住账户行。

不能只说“有事务就不会并发错”。两个事务都在事务里，也可能基于同一个旧余额做判断；必须结合锁、条件更新或乐观并发控制。

<a id="chapter-19"></a>

## 第 19 章　转账算法：幂等、悲观锁、死锁与双边流水

### 19.1 为什么幂等是金融接口的基本要求

客户端发送转账后可能在响应返回前超时。它不知道服务端到底没执行、正在执行还是已经完成，只能重试。如果每次 POST 都重新扣款，会重复转账。

幂等键表示“这几次重试属于同一个业务意图”。客户端重试必须复用原键；新的业务请求使用新键。

### 19.2 完整算法顺序

1. 校验幂等键非空且不超过 64 字符。
2. 拒绝付款与收款账号相同。
3. 无锁快速查询 request_id；若存在，比较载荷并重放旧结果。
4. 把两个账号按字典序排序。
5. 按固定顺序对两个账户执行悲观写锁查询。
6. 锁后再次查询 request_id，关闭并发窗口。
7. 映射回真实付款方和收款方。
8. 获取统一微秒时间。
9. 付款方扣款并检查状态/余额；收款方入账。
10. 创建转账订单。
11. 创建 DEBIT/CREDIT 两条流水和余额快照。
12. 把订单状态推进到 COMPLETED。
13. 事务提交；JPA 脏检查生成 UPDATE。

### 19.3 为什么先快查，再锁后双检

多数重放发生在原请求已经完成后。第一次查询能直接返回旧结果，不需要再争抢账户锁。

但两个相同幂等键的请求可能同时到达，都在第一次查询时没看到订单。只有获得账户锁后再次检查，后来的请求才能看到前一个已提交订单并重放。

数据库 request_id 唯一约束仍是最终防线。多层保护不是重复浪费：快速路径解决性能，锁后双检关闭主要竞态，唯一约束防御遗漏和其他写入口。

### 19.4 同一键不同载荷为什么返回冲突

若 request-id-1 第一次表示 A 向 B 转 100，后来却表示 A 向 C 转 500，服务不能猜测哪个意图正确，也不能静默返回旧订单。项目比较付款、收款和金额；不一致返回 409 `IDEMPOTENCY_KEY_CONFLICT`。

金额用 `compareTo`，所以 10.0 与 10.00 按数值相等。

### 19.5 悲观锁怎样防止超扣

`findByAccountNoForUpdate` 在事务中形成 `SELECT ... FOR UPDATE`。一个事务锁住付款账户后，另一个修改同一账户的事务等待；先提交后，后来者读取最新余额再判断。

悲观锁适合冲突概率较高、业务不能容忍重试逻辑复杂的关键余额更新；代价是等待、锁竞争和潜在死锁。低冲突场景可用乐观锁 + 重试，或数据库条件更新。

### 19.6 为什么固定账号顺序能降低死锁

假设事务一 A->B 先锁 A 再等 B；事务二 B->A 先锁 B 再等 A，两者形成环形等待。项目不按付款/收款角色加锁，而是两个账号字典序小的先锁、大的后锁。所有事务遵守同一全局顺序，破坏环形等待条件。

这能显著降低这类死锁，不代表数据库中永远不会出现任何死锁。其他表、索引范围和外部事务顺序仍可能产生死锁；生产代码还需识别死锁错误并进行有限、带退避的重试。

### 19.7 `@Version` 与悲观锁为什么同时存在

Account 实体还有 `@Version` 乐观锁字段。转账主路径明确使用悲观锁；version 为其他可能的账户更新入口提供额外并发检测。

不要含糊地说项目“同时靠两种锁保证同一件事”。应说明主路径依赖 `FOR UPDATE`，version 是实体级额外保护；若未来所有写入口明确统一，需要评估是否简化。

### 19.8 锁等待与死锁排查

现象可能是接口延迟升高、504、事务回滚或数据库死锁日志。检查当前事务、锁等待、慢 SQL、连接池和调用堆栈。不要只把超时时间调大。

需要理解的 MySQL 工具包括 `SHOW ENGINE INNODB STATUS`、Performance Schema 锁表和慢查询日志；具体权限和版本视环境而定。实验必须在隔离库，用两个会话手工锁相同行，观察第二会话等待，再提交/回滚释放。

### 19.9 当前并发验证边界

代码实现了固定顺序悲观锁、锁后幂等双检和唯一约束，已有集成测试验证顺序重放、回滚和载荷冲突。但当前测试集没有真正启动多个线程同时轰击同一账户/幂等键，也没有性能压测数据。

因此简历可以说“设计并实现并发控制与幂等机制，并用真实 MySQL 集成测试验证重放和回滚”，不能说“已通过高并发压测”或给出虚构 TPS。

下一步应补：并发集成测试、死锁/锁等待指标、JMeter/k6 压测基线、数据库连接池与资源曲线。先建立小规模可重复实验，不追求夸张数字。

### 19.10 转账源码逐段调试练习

在 IDEA 中：

1. 以可连接 MySQL 的配置启动应用。
2. 在 `TransferController.transfer`、`TransferApplicationService.transfer`、`Account.debit`、`LedgerEntry.debit` 设置断点。
3. 创建两账户并发转账请求。
4. 观察 requestId、排序后的 first/second、source/target 映射、余额前后、订单状态。
5. 提交后在 MySQL 查三张表。
6. 使用同一键重放，确认断点走快速 replay 而不再 debit。
7. 用同一键改金额，确认 409。
8. 发起余额不足，确认数据库没有订单和流水。

### 19.11 面试复述模板

“转账接口把扣款、入账、订单和双边流水放在一个 MySQL 事务里。客户端用 Idempotency-Key 表达一次业务意图；服务先走无锁快查，再按账号固定顺序悲观锁住两个账户，锁后再次检查幂等记录，最后由 request_id 唯一约束兜底。固定锁顺序降低 A->B/B->A 的死锁风险；同键同载荷返回旧结果，同键不同载荷返回 409。余额不足抛运行时业务异常，事务整体回滚。当前验证了真实 MySQL 重放和回滚，但尚未宣称完成高并发压测。”

<a id="chapter-20"></a>

## 第 20 章　JPA/Hibernate：项目里实际发生了什么

### 20.1 实体映射

`@Entity` 把 Java 类映射到表，`@Column` 描述列，`@Id/@GeneratedValue` 描述主键，`@Enumerated(EnumType.STRING)` 以可读字符串保存枚举。实体需要无参构造器供 JPA 反射，但项目把它设为 protected，业务代码通过工厂方法创建完整对象。

### 20.2 持久化上下文与脏检查

事务内从 Repository 加载的实体处于受管理状态。调用 `source.debit` 和 `target.credit` 改字段后，即使没有显式 save，事务提交前 Hibernate 会比较变化并生成 UPDATE，这叫脏检查。

订单 `save` 后再 `complete`，状态变化同样由脏检查更新。理解这一点能解释“为什么代码没有每处都写 update SQL”。

### 20.3 LAZY 与 Open Session in View

流水到订单是 LAZY 关联。项目关闭 Open Session in View，并在服务事务内加载和转换 DTO，避免 Controller 返回 JSON 时才触发额外查询或 LazyInitializationException。

LAZY 不等于永远不会查询；访问关联时仍会加载。生产性能排查要关注 N+1 查询、抓取计划和 SQL 日志。

### 20.4 自动方法名查询与 JPQL

`findByAccountNo`、`findAllByStatusOrderByCreatedAtDesc` 等由方法名解析；汇总余额和金额使用 JPQL。选择方法名还是显式查询，取决于复杂度和可读性，不能把超长条件强塞进方法名。

### 20.5 DTO 映射和数据精度

实体不会直接出 Web 层。DTO 还可计算展示字段，例如流水响应根据方向反推 balanceBefore。时间写入前截断微秒，金额全链使用 BigDecimal/DECIMAL。

前端 TypeScript 当前把 amount/balance 声明为 number。浏览器对演示金额足够，但真正大额金融系统不应无条件用 IEEE 754 number 承载高精度金额，通常使用字符串或专用十进制库。这是当前个人项目的边界。

<a id="chapter-21"></a>

## 第 21 章　运维总览读模型：真实数据，不是静态大屏

### 21.1 为什么单独有 dashboard 模块

总览需要跨账户和转账汇总，但它只读、不修改资金。放在独立 dashboard 包可以避免 account/transfer 为展示互相污染职责。

### 21.2 汇总口径

接口返回生成时间、环境、版本、账户数、余额总和、订单总数、完成数、完成金额、成功率、24 小时趋势和最近 6 笔订单。

所有数字来自 MySQL 查询。前端不会用当前表格的有限行数反推总数，也不写死虚构延迟和备份体积。

### 21.3 24 小时零值桶

服务先建立最近 24 个整点的零值桶，再把订单归入对应小时。没有交易的小时也返回 0，前端趋势线连续。

当前实现会把 24 小时订单列表读入应用聚合；数据规模增长后，应考虑数据库按小时 group by、预聚合、缓存或时序/分析系统。但个人项目阶段这样更易读、数据量可控。

### 21.4 “成功率”边界

当前成功率 = COMPLETED 订单 / 所有已落库订单。因为失败事务不留下 FAILED 订单，在正常模型下它很可能长期接近 100%。它不能代表所有请求成功率，因为参数错误、余额不足、5xx 不一定落入 transfer_order。

更完整的业务成功率应结合 HTTP 指标、失败审计或显式失败状态。面试时要主动指出这个口径边界，而不是把 100% 当成生产成绩。

<a id="chapter-22"></a>

## 第 22 章　后端测试：怎样证明，而不是怎样自信

### 22.1 测试金字塔在当前项目中的落点

项目包含小型 Filter 测试、MockMvc 健康检查、真实 MySQL 集成测试和浏览器端到端测试。数量不大，但覆盖关键风险。

### 22.2 为什么不用 H2

转账依赖 MySQL 的锁、唯一约束、CHECK、时间精度和 Flyway 脚本。H2 的 SQL 方言与并发行为可能不同，测试通过不代表 MySQL 正确。Testcontainers 在测试时启动真实 MySQL 8.4，让迁移、JPA 和业务使用同类数据库。

代价是测试需要 Docker、启动更慢、环境故障更多。单元测试仍可对纯业务方法快速验证；关键数据库语义用 Testcontainers。

### 22.3 当前七项测试证明什么

- readiness 端点可用。
- 合法请求 ID 被保留，不安全 ID 被替换。
- 相同幂等键重放只产生一张订单、两条流水和一次余额变化。
- 余额不足整体回滚。
- 同键不同载荷被拒绝。
- 账户、订单、流水和运维总览读模型来自真实 MySQL，并验证趋势、总额和 balanced。

### 22.4 它们没有证明什么

- 没有真实多线程并发冲突测试。
- 没有负载/容量/长稳测试。
- 没有认证授权测试，因为功能未实现。
- 没有外部 MySQL 断网、主从切换和恢复点目标测试。
- 没有完整浏览器兼容矩阵。

### 22.5 构建与测试命令

```bash
mvn test
mvn package
mvn -DskipTests package
mvn dependency:tree
```

`package -DskipTests` 只证明跳过测试后能打包，不是正式验收。发布门禁应在同一代码版本上先运行测试，再构建不可变制品。

### 22.6 测试失败怎样排查

1. Docker Engine/Testcontainers 是否可用。
2. 容器启动和镜像拉取日志。
3. Flyway 迁移和实体校验。
4. 测试数据是否清理、唯一值是否冲突。
5. 时间精度和时区。
6. 断言是业务失败还是环境失败。

不要通过放宽所有断言“修复”测试，也不要把偶发环境失败误判为业务正确。

<a id="chapter-23"></a>

## 第 23 章　后端核心实验与验收

### 23.1 开户实验

正常：创建合法账号，观察 HTTP 201、响应 X-Request-ID、account 表一行、version 初值和时间。

异常一：相同账号再次创建，预期 409，数据库仍一行。

异常二：账号含字母或长度不足，预期 400，事务不进入数据库写入。

### 23.2 转账实验

正常：A=1000，B=100，转 250，预期 A=750、B=350、一订单、两流水、balanced=true，总余额仍 1100。

重放：同键同载荷再发，结果相同，余额和行数不再变化。

冲突：同键改金额，预期 409。

余额不足：预期 422，余额、订单和流水不变。

同账户：预期 422，不进入锁和写入。

### 23.3 数据一致性手工校验

```sql
SELECT SUM(balance) FROM account;
SELECT request_id, COUNT(*) FROM transfer_order GROUP BY request_id HAVING COUNT(*) > 1;
SELECT transfer_order_id,
       SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END) AS debit,
       SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END) AS credit,
       COUNT(*) AS entries
FROM ledger_entry
GROUP BY transfer_order_id;
```

这些查询是学习校验，不是完整会计对账系统。生产还需考虑期初、冲正、手续费、币种、冻结、日切和外部渠道。

### 23.4 必须能够回答

1. Controller、Service、Repository、Entity、DTO 各做什么？
2. 为什么前后端都校验，数据库还要约束？
3. 为什么 Flyway V1 不能改？
4. 为什么金额不用 double？
5. `@Transactional` 能自动解决所有并发问题吗？
6. 幂等快查、锁后双检、唯一约束分别解决什么？
7. 固定锁顺序为什么只是降低而非消灭所有死锁？
8. 当前成功率为什么不代表所有转账请求的成功率？
9. Testcontainers 比 H2 更真实在哪里，代价是什么？
10. 当前没有并发压测，你的简历措辞怎样保持诚实？

<a id="part-01"></a>

# 第五篇：用户看见什么——React 前端与真实演示闭环

<a id="chapter-24"></a>

## 第 24 章　前端架构：页面不是静态大屏

### 24.1 构建和运行方式

开发时 Vite 在 18100 提供热更新，并把 `/api` 与 `/health` 代理到本机 Spring Boot 18080。生产构建生成纯静态文件，放入 Nginx 镜像；浏览器仍请求相对路径，由 Nginx 代理后端。

相对路径使前端无需写死 `localhost:18080`，也避免 Compose 与 Kubernetes 使用不同 API 地址时重新构建 JavaScript。

### 24.2 应用壳与路由

`AppShell` 提供左侧导航、顶部环境、移动端抽屉和内容区。路由包括：

- `/` 运行总览。
- `/accounts` 账户管理。
- `/transfers` 转账中心。
- `/ledger` 流水审计。
- `/system` 系统状态。

错误路径回到总览；Nginx 的 SPA 回退保证直接刷新子路由仍返回 index.html。

### 24.3 API 客户端

统一 `request<T>` 封装 fetch：设置 JSON 请求头，检查 `response.ok`，尝试解析后端 ProblemDetail；如果代理 502/504 返回 HTML，JSON 解析失败后仍保留 HTTP 状态作为线索。

TypeScript 泛型只在编译期帮助前端开发，不能验证运行时服务器一定返回同样结构。生产可引入契约生成或运行时 schema 校验，但当前项目未实现。

### 24.4 账户管理页

页面能生成演示账号、提交真实 POST、刷新列表、按账号/名称在前端筛选并显示成功反馈。账号生成只降低手工冲突概率，数据库唯一约束才是最终防线。

正常演示：创建账户后立即在列表出现，随后能在转账页下拉框选择。失败演示：重复账号显示后端 409 的可读消息。

### 24.5 转账中心

页面同时加载账户和最近订单。表单选择付款/收款账户、金额和幂等键；成功后读取审计接口、刷新订单列表，并提供“用同一幂等键重放”按钮。

这一个按钮非常重要：它把抽象幂等机制变成现场可见证据。重放后服务端只有一张订单、两条流水，余额不再变化。

### 24.6 流水审计页

从订单列表选择一笔交易，读取 `/ledger`，并排展示 DEBIT/CREDIT、发生额、变动前后余额和借贷平衡结果。账户号在通用表格中默认脱敏显示，详细审计卡用于本地演示。

当前系统没有用户认证和数据权限，脱敏只是展示层处理，不是安全控制。真实银行系统必须在服务端按权限决定是否返回完整账号。

### 24.7 运行总览

总览一次请求读取服务端聚合数据，并独立请求 `/health`。四个状态块、业务指标、24 小时趋势和最近订单都来自 API/MySQL。

但页面中“应用正常、MySQL 正常、数据闭环正常”的呈现部分是由汇总请求成功和计数推断，不是完整组件级监控。真正的 JVM、HTTP、容器和主机指标在 Grafana。前端业务总览不能替代观测系统。

### 24.8 系统状态页与监控链接

页面根据后端返回的 environment 判断是否为 Compose。只有 `environment=compose` 才显示宿主机 13000/19090/19093 链接；Kubernetes 模式不再假装本地 Compose 端口存在。

这正是之前“监控链接打不开”问题的根源之一：页面链接只是入口，服务必须实际启动，而且 Minikube 与 Compose 不是同一运行环境。链接存在不等于服务在线。

### 24.9 加载、错误和无数据状态

每页有加载、错误、重试和空状态。异常不能只在控制台打印，也不能让用户看到无限转圈。按钮提交期间禁用，避免用户无意识重复操作；真正防重仍由服务端幂等承担。

### 24.10 响应式与可访问性

导航支持移动抽屉；表格可横向容纳；状态不只依赖颜色，还使用图标与文字；提供 skip link、aria label 和键盘行选择。

历史验收记录显示 Edge 1440×1000 与 390×844 下无横向溢出和浏览器错误。它是当时证据，不代表所有浏览器和屏幕已覆盖。

<a id="chapter-25"></a>

## 第 25 章　前端构建、联调与故障排查

### 25.1 开发与生产差异

开发：浏览器 -> Vite 18100 -> 代理 Spring Boot。生产：浏览器 -> Nginx 18000/8080 -> Spring Boot。若开发正常、容器失败，优先比较代理地址、环境、Nginx 配置和构建产物，而不是立即修改 React 业务逻辑。

### 25.2 常见故障

#### 白屏

看浏览器 console、network、静态资源 404、JS 运行异常、base path 和缓存。Nginx 首页 200 不代表 JS bundle 成功加载。

#### 页面能开，API 502

静态 Nginx 正常，upstream 失败。按 Nginx -> API -> MySQL 检查。

#### API 200，但页面字段空

比较 JSON 字段名与 TypeScript interface/组件读取字段，检查时区、数字格式和可选字段。TypeScript 不会验证运行时 JSON。

#### 刷新子路由 404

检查 Nginx `try_files ... /index.html`。

#### 修改代码没有生效

开发看 Vite 热更新；生产需重新 `npm run build` 并重建 Web 镜像。浏览器缓存和旧容器也可能提供旧文件，通过文件哈希、镜像 ID和版本信息确认。

### 25.3 Edge 端到端测试

脚本在 1440×1000 下打开项目，检查四个业务指标，创建两个账户，完成转账和重放，确认两张流水卡；再直接请求服务器验证同 requestId 只有一订单、balanced=true；最后切到 390×844 检查移动导航，并捕获 console、pageerror 和 HTTP 4xx/5xx。

端到端测试证明用户路径能连通多层，但失败定位成本较高，不能替代后端集成测试和组件测试。

### 25.4 前端学习验收

- 能画出 Vite 开发代理与 Nginx 生产代理的差异。
- 能在浏览器 Network 中找到一笔 POST 的请求头、JSON、状态码和请求 ID。
- 能从页面错误判断是前端运行错误、代理错误还是业务 4xx。
- 能修改一个展示字段或表单校验，并完成构建和联调。
- 能解释为什么前端校验、按钮禁用和服务端幂等不能互相替代。

<a id="part-02"></a>

# 第六篇：把系统变得可观察——指标、日志与告警

<a id="chapter-26"></a>

## 第 26 章　可观测性到底解决什么问题

### 26.1 监控不是“装几个工具”

监控系统应回答：服务是否可用？流量怎样？错误是否增加？响应是否变慢？资源是否接近上限？问题从何时开始？与哪个版本相关？哪一类请求受影响？

OpsPilot 用指标回答“发生了多少、趋势怎样”，用日志回答“某次请求具体发生什么”，用告警把持续异常变成需要处理的事件。

### 26.2 四个黄金信号

- Latency 延迟：请求耗时，需区分成功/失败和分位数。
- Traffic 流量：每秒请求、业务量。
- Errors 错误：5xx、业务失败、抓取失败。
- Saturation 饱和度：CPU、内存、磁盘、连接池等接近容量。

这些是观察框架，不是固定仪表盘模板。项目当前覆盖 HTTP 请求率/P95、JVM 堆、服务 up、磁盘等，尚未覆盖所有数据库连接池和业务失败指标。

### 26.3 指标、日志、追踪

指标是按时间采样的数值，适合聚合趋势和告警；日志是离散事件，适合上下文和错误细节；分布式追踪把一次请求跨多个服务拆成 span。

当前项目有请求 ID 日志关联，但没有 OpenTelemetry/Tempo 的标准分布式追踪。模块化单体阶段链路较短，先把指标与日志做实，再在服务拆分或跨系统调用增多时引入追踪。

### 26.4 健康检查与业务正确

健康端点回答的是编排系统是否应接流量/重启进程，不是完整业务验收。Prometheus `up=1` 只表示抓取指标成功；它不能证明一笔转账金额正确。

所以发布门禁至少分三层：进程/探针、依赖/入口、关键业务冒烟。

<a id="chapter-27"></a>

## 第 27 章　Actuator 与 Micrometer

### 27.1 Actuator 暴露什么

项目只公开 health、info、prometheus。Nginx 对外隐藏 `/actuator/*`，只把 `/health` 映射为 readiness；Prometheus 在内部访问 `/actuator/prometheus`。

管理端点可能泄露环境、指标和内部结构，不能默认全量公开。生产还可将管理端口分离、加认证、网络限制和审计。

### 27.2 startup、readiness、liveness

- startup：应用是否仍在允许的启动窗口。失败到阈值后才重启，保护慢启动。
- readiness：当前实例是否适合接收新流量。失败时从服务端点摘除，不一定重启。
- liveness：进程是否失去自我恢复能力。失败时重启。

不要把短暂外部数据库故障加入 liveness，否则所有实例可能一起重启形成级联故障。Spring Boot 默认 readiness 也不自动包含所有外部依赖；是否加入数据库需要按服务是否还能提供降级能力判断。项目文档把 readiness 描述为含数据库依赖，但当前配置没有显式 `management.endpoint.health.group.readiness.include=db,...`，因此不能仅凭注释断言 readiness 一定检查 MySQL。这是需要后续实测并修正配置或文档的细节。

### 27.3 Micrometer

Micrometer 为不同监控后端提供统一指标 API。Spring MVC、JVM、Tomcat、数据源等自动产生指标，Prometheus Registry 将它们转换为文本格式。

项目为所有指标增加 application、environment、version 标签，并为 HTTP 延迟开启直方图和 SLO 桶。标签便于筛选，但高基数值（requestId、用户 ID、完整 URL）不能作为指标标签，否则时序数量爆炸。

### 27.4 P95 是什么

P95 表示约 95% 请求耗时不超过这个值，不是“最慢请求”，也不是平均值。Prometheus 从 histogram bucket 的速率用 `histogram_quantile` 估算。

必须先按 `le` 合并实例再计算全局分位数。客户端摘要分位数通常不能跨实例直接平均。

<a id="chapter-28"></a>

## 第 28 章　Prometheus：抓取、时序、PromQL 与规则

### 28.1 工作模式

Prometheus 每 15 秒主动抓取目标，保存带时间戳和标签的样本；每 15 秒计算规则。目标包括自身、OpsPilot、Node Exporter、cAdvisor 和 Loki。

主机端口 19090 只是映射，容器间 Alertmanager 使用 `alertmanager:9093`，不能在 Prometheus 容器中写 localhost:19093。

### 28.2 Target 页面怎样用

Targets 显示 job、endpoint、last scrape、duration、state 和错误。DOWN 时先读 last error：连接拒绝、DNS、超时、404、解析错误对应不同层。

`up{job="opspilot"}` 是 Prometheus 为抓取自动生成的指标，1 表示最近抓取成功，0 表示失败。它监控的是指标端点可达，不是“所有接口都正确”。

### 28.3 Counter、Gauge、Histogram

- Counter 只递增（进程重启可归零），如请求总数；通常用 rate 计算单位时间速率。
- Gauge 可升可降，如当前内存、线程数。
- Histogram 把观测值计入多个 bucket，并有 count/sum，可计算分位数和 SLO。

看到 `_total`/`_count` 不应直接用两个时刻手算，PromQL `rate` 能处理采样与重置。

### 28.4 项目常用 PromQL

服务状态：

```promql
up{job="opspilot"}
```

排除 Actuator 的每秒请求：

```promql
sum(rate(http_server_requests_seconds_count{application="opspilot",uri!~"/actuator.*"}[1m]))
```

5xx 比例：

```promql
sum(rate(http_server_requests_seconds_count{application="opspilot",status=~"5.."}[5m]))
/
clamp_min(sum(rate(http_server_requests_seconds_count{application="opspilot"}[5m])), 0.001)
```

P95：

```promql
histogram_quantile(
  0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="opspilot"}[5m]))
)
```

### 28.5 告警规则

项目定义：

- OpsPilotDown：up=0 持续 1 分钟，critical。
- HighErrorRate：5xx 比例 >5% 持续 5 分钟。
- HighP95Latency：P95 >1 秒持续 5 分钟。
- JvmHeapHigh：堆使用 >85% 持续 10 分钟。
- HostDiskSpaceLow：可用空间 <10% 持续 10 分钟。

`for` 用于过滤短暂抖动，但会增加发现延迟。阈值应结合 SLO、流量和容量调整；本地演示值不是银行生产标准。

### 28.6 为什么接口 4xx 不一定触发 5xx 告警

余额不足是 422，表示服务正常执行并拒绝业务，不应计入服务器 5xx。若业务失败率重要，应单独建立业务错误指标，而不是把所有 4xx 当系统故障。

### 28.7 Prometheus 故障

- 页面打不开：容器是否运行、端口 19090 是否绑定、是否被其他模式占用。
- Target down：按 last error 定位 DNS/TCP/HTTP/格式。
- Rule 加载失败：检查 YAML 和 PromQL，Prometheus 日志会拒绝错误规则。
- 查询无数据：时间范围、标签名、指标是否实际产生、抓取是否成功。
- 磁盘增长：检查 7 天保留、样本基数、卷空间；不能随意删 TSDB 文件。

<a id="chapter-29"></a>

## 第 29 章　Grafana：看板怎样成为证据

### 29.1 数据源供应

Grafana 启动时自动创建 Prometheus 和 Loki 数据源。数据源 URL 使用 Compose 服务名；浏览器请求先到 Grafana 后端，再由 Grafana 容器访问数据源。

看板 JSON 和 provisioning 只读挂载，Git 中配置是事实来源；UI 临时修改不能保存覆盖。这样环境重建后可重复。

### 29.2 当前七个面板

应用状态、每秒业务请求、JVM 堆使用率、HTTP P95、按 URI/状态的请求趋势、JVM 内存趋势、OpsPilot 应用日志。

看板默认最近 30 分钟、10 秒刷新。若刚启动没有业务流量，请求率/P95 可能无数据，这不是自动故障；先执行一组业务请求。

### 29.3 怎样从告警走到看板

以高延迟为例：先确认告警开始和恢复时间、环境和版本；看请求率是否突增、错误是否一起上升；看 JVM/容器/主机资源；按 URI 分解；再用同一时间范围查日志和数据库。不要只盯一个红色数字。

### 29.4 登录与密码

本地默认管理员和环境密码用于学习；正式环境必须覆盖、受控保存和轮换。Grafana 未开启匿名访问和自注册，但本地认证不能代替生产 SSO、RBAC 与审计。

<a id="chapter-30"></a>

## 第 30 章　日志链路：Alloy、Loki 与请求 ID

### 30.1 完整链路

Java/Nginx/MySQL 等容器写 stdout/stderr -> Docker log driver -> Alloy 通过只读 Docker socket 发现 `project=opspilot` 容器 -> 添加 `service/container/job` 低基数标签 -> 推送 Loki -> Grafana 使用 LogQL 查询。

### 30.2 Alloy 为什么需要 Docker socket

它用 socket 读取容器元数据和日志。即使以只读方式挂载，Docker socket 仍是高权限接口，生产应严格限制采集器身份、镜像和主机访问，不把它暴露给普通应用。

### 30.3 Loki 怎样存日志

Loki 主要索引标签，正文存日志块。项目是单节点、文件系统存储、7 天保留、关闭多租户鉴权，只适合回环绑定的学习环境。生产通常需要认证/租户隔离、对象存储、容量和高可用规划。

### 30.4 LogQL 最小用法

应用日志：

```logql
{service="app"}
```

按请求 ID 过滤：

```logql
{service="app"} |= "learn-transfer-001"
```

不要把 requestId 直接做 Loki 标签，因为每次请求都不同，会形成高基数。作为日志正文过滤更合适。

### 30.5 日志查不到

依次检查：目标容器是否产生日志、Alloy discovery 是否发现、读取位置、推送是否成功、Loki ready、Grafana 数据源、时间范围和标签。容器刚重建时 container 名和读取位置可能变化，Alloy 存储卷用于减少重复/漏采。

### 30.6 日志规范

至少包含时间、级别、线程、traceId、类和消息。错误日志要有上下文但不能泄露密码、完整敏感账号和 Token。堆栈只记录一次关键位置，避免多层重复造成噪声。

<a id="chapter-31"></a>

## 第 31 章　Alertmanager：它到底是干什么的

### 31.1 与 Prometheus 分工

Prometheus 根据指标和规则判断告警状态；Alertmanager 接收告警后分组、去重、静默、抑制和路由到接收器。

`http://localhost:19093/#/alerts` 显示收到的告警。没有告警时页面空是正常，不是不会用；先在 Prometheus Alerts 页面确认规则状态与是否发送。

### 31.2 Pending、Firing、Resolved

表达式首次满足但未达到 `for` 时是 Pending；持续超过 for 后 Firing；条件恢复后 Resolved。Alertmanager 的 resolve timeout 处理长时间未再收到更新的情况。

### 31.3 分组和静默

项目按 alertname、job、severity 分组，减少多个实例同时故障导致通知风暴。Silence 按匹配条件在指定时间内静默，适合已知维护窗口；静默不是删除规则，必须写明原因、负责人和到期时间。

### 31.4 当前边界

接收器 `local-ui` 是空配置，告警只保留在 UI，未接邮件、企业微信、短信或 PagerDuty。因此项目已实现规则与 Alertmanager 流程，但不能说已经完成外部通知闭环。

补齐时应使用测试渠道，设计严重级别、值班路由、去重、升级、维护静默和通知恢复；外发消息属于真实外部动作，需明确授权。

<a id="chapter-32"></a>

## 第 32 章　可观测性实验与验收

### 32.1 正常请求关联

1. 发请求时设置 `X-Request-ID: observe-001`。
2. 确认响应返回同一 ID。
3. 在 Loki 用 `|= "observe-001"` 找日志。
4. 在 Prometheus/Grafana 同时间范围观察请求数。
5. 在 MySQL 查询对应业务 request_id（转账时可让二者相同，便于学习，但二者概念不同）。

### 32.2 停服务告警

仅在实验环境使用故障注入脚本并输入明确确认口令。停止应用超过规则 for，观察 Prometheus Pending -> Firing、Alertmanager 收到、恢复后 Resolved，并记录从故障开始到发现/恢复的时间。

### 32.3 高错误率实验边界

业务 4xx 不触发 5xx 规则。要演示 5xx 必须在隔离环境制造受控服务端错误，不能污染共享数据。实验后恢复并核对错误率窗口逐渐下降。

### 32.4 必须回答

1. Prometheus 和接口文档有什么区别？
2. up=1 能证明什么、不能证明什么？
3. Alertmanager 为什么页面可能为空？
4. 指标和日志分别适合回答什么？
5. 为什么 requestId 不应做指标/Loki高基数标签？
6. readiness 与 liveness 为什么不能互换？
7. 当前外部告警通知为什么不能写成已完成？

<a id="part-01"></a>

# 第七篇：从单机容器到 Kubernetes

<a id="chapter-33"></a>

## 第 33 章　为什么有 Compose 还要 Kubernetes

### 33.1 Compose 与 Kubernetes 的边界

Compose 擅长在一台机器上复现多服务；Kubernetes 用控制器持续维护集群中的期望状态，支持多副本、滚动发布、服务发现、资源调度、故障摘流和权限/网络治理。

Kubernetes 不会自动让错误代码变正确，也不会把单副本数据库变高可用。它提高的是运行和交付能力，同时引入更多对象、网络和控制面故障。

### 33.2 从 Docker 对象映射到 K8s

| Docker/Compose | Kubernetes 对应 | 关键差异 |
| --- | --- | --- |
| Container | Container in Pod | Pod 是调度与网络最小单位 |
| service container | Pod + Controller | Pod 可替换，控制器维护副本 |
| Compose service name | Service | 提供稳定虚拟地址和服务发现 |
| port mapping | Service/Ingress/port-forward | 内外入口分层 |
| named volume | PVC/PV/StorageClass | 存储独立于 Pod 生命周期 |
| env/config file | ConfigMap/Secret | 配置对象和敏感对象分离 |
| restart policy | Controller + kubelet | 从单容器重启扩展为期望状态调谐 |

### 33.3 声明式与控制循环

你提交 Deployment 描述“需要两个 API Pod”。Kubernetes 控制器不断比较期望状态与实际状态：少一个就创建，多一个就处理，模板变化就滚动替换。

`kubectl apply` 成功只代表 API Server 接受对象，不代表 Pod 已 Ready；后续还可能拉镜像失败、调度失败、探针失败或配置错误。因此必须继续看 rollout、Pod、Events 和日志。

<a id="chapter-34"></a>

## 第 34 章　Pod、Deployment、ReplicaSet：应用怎样自愈

### 34.1 Pod

Pod 包含一个或多个共享网络命名空间和部分存储的容器。OpsPilot API Pod 含一个 initContainer 和一个 API 容器；Web Pod 含 Nginx 容器。

Pod 名和 IP 是可变的。不能把 Pod IP 写入前端；由 Service 选择 Ready Pod。

### 34.2 initContainer

API 的 `wait-for-database` 使用 API 镜像内的 `nc` 循环检查数据库 TCP 端口，成功后主容器才启动。它减少首次并行创建时 Flyway 无意义失败。

边界：端口开放不代表数据库用户可认证或 schema 正确。真正连接仍由应用完成。若数据库永久不可达，initContainer 会持续等待，需要从其日志、DNS、Service、NetworkPolicy 和数据库 Pod 排查。

### 34.3 Deployment 与 ReplicaSet

API 和 Web 是 Deployment，默认各两副本。Deployment 管理 ReplicaSet，ReplicaSet 管理 Pod。删除一个受控 Pod，控制器创建新 Pod 恢复副本，这是自愈演示。

不要把“Pod 重启”与“Pod 被替换”混为一谈：容器重启可能保持 Pod 名，控制器重建会产生新 Pod UID/名称和 IP。

### 34.4 滚动更新

`maxUnavailable=0`、`maxSurge=1` 表示升级时不主动降低可用副本，最多额外创建一个新 Pod，等新 Pod Ready 再替换旧 Pod。

它需要额外容量；资源不足时新 Pod 可能 Pending，rollout 卡住。readiness 错误可能让新版本永远不进入 Service，保护旧容量；liveness 错误可能形成重启循环。

### 34.5 revisionHistoryLimit

Deployment 保留 5 个 ReplicaSet 修订用于工作负载级回滚。Helm 还保存整个 Release 的多资源历史。二者层级不同：Deployment revision 管单个工作负载模板，Helm revision 管 Chart 渲染出的整套资源。

<a id="chapter-35"></a>

## 第 35 章　Service、EndpointSlice 与 Ingress

### 35.1 Service

`opspilot-backend` ClusterIP Service 通过标签选择 API Pod，在 18080 提供稳定入口；`opspilot-web` 选择 Web Pod，在 8080 提供入口。

Service 没有可用后端时仍可能存在 ClusterIP。要看 EndpointSlice 是否包含 Ready Pod 地址。

```bash
kubectl -n opspilot get svc
kubectl -n opspilot get endpointslice
kubectl -n opspilot describe svc opspilot-backend
```

### 35.2 标签和选择器

Service/Deployment/NetworkPolicy 都依赖标签。selector 拼错时 Pod 可能健康但 Service 无端点。排障要对比 Pod labels 与 Service selector，而不是只重启。

### 35.3 Ingress

Ingress 声明域名/路径到 Service 的 HTTP 路由，但它必须有可工作的 Ingress Controller。只有 Ingress 对象，没有 Controller，不会产生实际流量入口。

本地 Chart 有 `opspilot.local` 模板，并通过 lint 和服务端 dry-run；历史实机因 Ingress Controller 镜像与残留 webhook 问题，Release 使用 `ingress.enabled=false`，通过 port-forward 验收。因此不能说“本机 Ingress 已实际跑通”。

### 35.4 port-forward

```powershell
kubectl -n opspilot port-forward service/opspilot-web 18000:8080
```

它在本机建立临时转发，适合调试和验收；终端关闭即停止，不是生产入口，不具备负载均衡器、域名、TLS 和高可用能力。

<a id="chapter-36"></a>

## 第 36 章　ConfigMap、Secret 与配置变更

### 36.1 为什么分离

ConfigMap 保存环境、版本、DB 地址、用户名等非敏感配置；Secret 保存数据库密码。分离便于权限和生命周期控制。

Kubernetes Secret 默认只是 base64 编码，不是自动加密。生产需要 etcd 静态加密、RBAC、外部密钥平台、轮换和审计。

### 36.2 envFrom 与 secretKeyRef

API 从 ConfigMap 批量导入环境变量，DB_PASSWORD 从 Secret 单键读取。工作负载不挂载 Kubernetes API Token，减少不必要权限。

### 36.3 配置更新为什么触发滚动

Pod template annotations 包含 ConfigMap/Secret 渲染内容的校验和。配置变化时 template hash 变化，Deployment 创建新 ReplicaSet。否则只修改 ConfigMap，已有以环境变量启动的 Pod 不会自动重读。

### 36.4 生产 Secret 模式

默认 values 创建本地 Secret，并含演示密码；production example 设置 `create=false`，引用由 Vault/云密钥/平台团队预先创建的 existingSecret。

不能把明文演示 values 当生产密钥管理。Git 历史中的秘密即使后续删除也可能残留，必须轮换。

<a id="chapter-37"></a>

## 第 37 章　StatefulSet、PVC 与 MySQL 边界

### 37.1 为什么 MySQL 不用 Deployment

StatefulSet 为 Pod 提供稳定序号和与实例绑定的卷。MySQL 为单副本 `mysql-0`，通过 Headless Service 获得稳定网络身份，数据写 5Gi PVC。

Pod 重建后重新挂载 PVC，证明数据生命周期独立于 Pod。

### 37.2 PV、PVC、StorageClass

PVC 是应用对存储的请求；PV 是实际存储资源；StorageClass 定义动态供应方式。Minikube 默认存储可让 5Gi PVC Bound。

`ReadWriteOnce` 表示通常单节点读写挂载，不等于只能一个 Pod 读，也要结合驱动语义。

### 37.3 单副本不是高可用

PVC 能防 Pod 重建丢数据，却防不了节点/磁盘故障、数据库进程长期不可用、误操作或区域灾难。一个 StatefulSet Pod 不等于复制、自动故障转移和备份恢复。

类生产 values 关闭内置 MySQL，连接企业托管/高可用数据库。若自建，应采用成熟 Operator 或受控主从/集群方案，并验证备份、恢复、RPO/RTO、故障转移和一致性。

### 37.4 删除资源的风险

删除 StatefulSet 不一定删除 PVC；删除 Helm Release 对 PVC 的行为需看资源策略；手工删 PVC 可能导致数据不可恢复。任何清理前先列出并确认备份，不把卸载命令写成无脑一键删除。

<a id="chapter-38"></a>

## 第 38 章　三类探针与优雅摘流

### 38.1 API 探针

startup 每 5 秒，最多 30 次失败，为启动留出约 150 秒；readiness 每 5 秒，连续 3 次失败摘流；liveness 每 10 秒，连续 3 次失败重启。

API preStop 先 sleep 5 秒，给 EndpointSlice 摘流传播，再由终止信号触发 Spring Boot 优雅停机。terminationGracePeriod 35 秒，大于 Spring 20 秒停机窗口。

### 38.2 Web 探针

startup/readiness 访问 `/health`，覆盖 Nginx 到 API；liveness 访问 `/`，只验证 Nginx 静态服务。这样后端暂时失败时 Web Pod 会不就绪，但不会因为共享后端问题不停重启 Nginx。

### 38.3 探针设计风险

- liveness 依赖共享数据库：数据库故障导致所有 Pod 重启风暴。
- readiness 过严：所有实例同时摘流，用户完全不可用。
- timeout 太小：高负载时健康请求本身超时。
- 路径与业务入口不同：探针绿但真实链路坏。
- 同一重型检查被高频调用：健康检查反而制造负载。

当前 readiness 数据库包含关系需要实测和配置澄清，不能只依赖代码注释。

<a id="chapter-39"></a>

## 第 39 章　requests、limits 与 HPA

### 39.1 requests 和 limits

request 用于调度和 HPA CPU 利用率基准；limit 限制最大资源。API request 150m/256Mi，limit 1 CPU/768Mi；Web 更小；MySQL request 200m/512Mi，limit 1 CPU/1Gi。

CPU 超过 limit 会被节流；内存超过 limit 可能 OOMKilled。request 过低会让节点过度承诺，过高会使 Pod Pending。值应由测量调整。

### 39.2 HPA

API HPA 目标 2 至 4 副本，平均 CPU 70%。扩容窗口短、缩容稳定 300 秒，减少抖动。

CPU 利用率通常相对 request 计算；request 不合理会影响 HPA。HPA 还依赖 Metrics Server。历史本机 Metrics Server 镜像未成功，因此 HPA `TARGETS=unknown`，只能说对象和策略存在，不能说实际触发扩容。

### 39.3 本地扩容实验条件

先确认 `kubectl top pods` 有数据；再使用受控压测产生持续 CPU；观察 HPA target、desired replicas、Deployment/Pod 变化和服务延迟；停止负载后等待缩容窗口。无 Metrics Server 时不要制造压力却声称 HPA 工作。

<a id="chapter-40"></a>

## 第 40 章　PDB、拓扑与计划中断

### 40.1 PDB

API/Web PDB `minAvailable=1`，在 voluntary disruption（如 drain）时阻止同时驱逐所有副本。它不防节点突然宕机、容器崩溃，也不主动创建副本。

Minikube 单节点时 PDB 无法证明跨节点维护保护；可能反而让 drain 卡住。真实价值需要多节点和正确副本分布。

### 40.2 topologySpreadConstraints

配置尝试按 hostname 均匀分布副本，`ScheduleAnyway` 是软约束。单节点环境所有 Pod 仍在同一节点，所以两副本不等于节点高可用。

<a id="chapter-41"></a>

## 第 41 章　RBAC 与 ServiceAccount

### 41.1 工作负载身份

API/Web/MySQL 使用各自 ServiceAccount，但 `automountServiceAccountToken=false`，因为业务不需要调用 Kubernetes API。避免 Token 进入容器能缩小泄露面。

### 41.2 operator 最小权限

单独 `opspilot-operator` 可 get/list/watch Pods、日志、Service、Endpoint、Event、ConfigMap 和工作负载状态，并可 patch Deployment 用于受控 restart；不能读 Secret、删除 Pod或修改数据库 StatefulSet。

历史验收验证了可读 Pod、不可读 Secret、不可删除 Pod。RBAC 不是“给运维 cluster-admin 才方便”，最小权限是企业环境基本原则。

### 41.3 验证

```bash
kubectl auth can-i get pods --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
kubectl auth can-i get secrets --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
kubectl auth can-i delete pods --as=system:serviceaccount:opspilot:opspilot-operator -n opspilot
```

只检查 Role YAML 不如让 API Server 实际回答权限。

<a id="chapter-42"></a>

## 第 42 章　NetworkPolicy

### 42.1 当前策略

先对带 OpsPilot 标签的 Pod 默认拒绝入站，再放行：外部到 Web 8080、Web 到 API 18080、API 到 MySQL 3306。

它把架构图转化为网络最小权限：Web 不应直连数据库，其他 Pod 不应绕过 Web 随便调用 API。

### 42.2 CNI 边界

NetworkPolicy 只有在集群网络插件支持并执行时才有效。对象创建成功不等于规则真正被执行。Minikube 驱动/CNI 模式必须确认。

当前只设置 Ingress 策略，没有默认拒绝 Egress。生产还需考虑 DNS、外部数据库、监控抓取、Ingress Controller namespace、备份和必要出口，不能简单全拒绝后让应用失联。

### 42.3 策略排障

Service 有端点但连接超时，检查源/目标 Pod 标签、namespaceSelector/podSelector 组合、端口和 CNI。临时删除策略会扩大网络权限，不应在生产无审批操作；先用测试 Pod和策略描述收集证据。

<a id="chapter-43"></a>

## 第 43 章　Helm：参数化安装、升级和回滚

### 43.1 Chart 结构

`Chart.yaml` 描述名称和版本；`values.yaml` 是本地默认；production example 演示外部数据库与镜像仓库；templates 根据 values 生成 K8s 对象；NOTES 给出安装后提示；tests 含 Release Test Pod。

### 43.2 模板与 values

同一模板通过 values 改镜像标签、副本、资源、Ingress、数据库模式和 Secret。参数化避免复制多套 YAML 漂移，但模板复杂后必须 lint、render、server dry-run 和实装验证。

### 43.3 lint、template、dry-run

- `helm lint` 检查 Chart 结构和部分模板问题。
- `helm template` 本地渲染，不访问集群。
- Kubernetes server dry-run 让 API Server 验证资源 schema/准入。
- 真正 install/upgrade 才验证调度、镜像、探针、网络和运行。

它们是逐层门禁，不能用 lint 通过代替实机运行。

### 43.4 Release 与 revision

Helm Release `opspilot` 安装在 namespace `opspilot`。每次成功/失败升级形成 revision。`helm history` 显示 deployed、superseded、failed 等状态；回滚脚本选择最近可用历史，等待 API/Web rollout。

数据库 schema 变化可能使应用回滚不兼容，因此“Helm 能回滚”不等于任何版本都能安全回滚。迁移应尽量向前/向后兼容，破坏性变更分阶段。

### 43.5 Helm test

测试 Pod 使用 Web 镜像访问 `opspilot-web:8080/health`，经过 Nginx 到 API readiness。成功后清理，失败时保留日志。

它验证集群内入口健康链，但不是完整业务。另有 smoke-test 脚本创建账户、转账、重放和流水平衡，二者互补。

<a id="chapter-44"></a>

## 第 44 章　Minikube 一键部署脚本逐步拆解

### 44.1 前置检查

脚本检查 docker、minikube、kubectl、helm、mvn、npm；启动或复用 Docker driver Minikube；可选启用 ingress 和 metrics-server。

机器资源要同时容纳 Minikube VM/容器、API/Web/MySQL、插件和宿主工具。资源不足会出现 Pod Pending、镜像构建慢或系统卡顿。历史脚本默认曾使用 6GiB，学习规划也考虑过 4GiB约束，实际执行以当前脚本和机器可用资源为准。

### 44.2 构建和加载镜像

先生成 JAR/dist，构建 API/Web runtime 镜像，再用 `minikube image load` 加载 API、Web 和 MySQL，减少集群拉取公网失败。

本地镜像更新后必须重新 load；标签相同且 `IfNotPresent` 时可能仍用旧镜像，因此推荐使用新版本标签或明确检查镜像 ID。

### 44.3 Helm 部署门禁

脚本使用 upgrade/install、等待和失败回滚；随后分别等待 API/Web rollout、执行 Helm test、列出资源。正常结束不只看 Helm 命令返回 0。

### 44.4 降级路径

镜像网络或 Ingress webhook 异常时可 `-SkipAddons -DisableIngress`，保留核心 API/Web/MySQL/Service，通过 port-forward 验收。降级路径必须在简历和演示中说清，不应删除集群安全 webhook 来伪造成功。

<a id="chapter-45"></a>

## 第 45 章　Kubernetes 排障百科

### 45.1 通用顺序

```text
资源状态 -> Events -> describe -> 容器/initContainer 日志
-> Deployment/ReplicaSet -> Service/EndpointSlice
-> 探针 -> ConfigMap/Secret 引用 -> NetworkPolicy
-> 节点资源/存储 -> 恢复与复盘
```

### 45.2 Pending

Pod 未调度/未运行。看 Events：资源不足、PVC 未绑定、节点选择/污点、镜像拉取前置等。Pending 不等于应用代码错误。

### 45.3 ImagePullBackOff

先 `describe pod` 和 Events，区分镜像不存在、认证、DNS、代理和连接失败。自研镜像可重新 build/load；私有仓库用 imagePullSecret；不要反复删 Pod，控制器只会再次遇到同一问题。

### 45.4 CrashLoopBackOff

容器启动后反复退出。看当前和 previous logs、退出码、OOMKilled、环境、Flyway、权限、只读文件系统。`kubectl logs --previous` 常能看到上一轮崩溃根因。

### 45.5 Init:0/1 或 Init:CrashLoopBackOff

查看 initContainer 日志。OpsPilot 可能在等待数据库 DNS/端口；检查 mysql Service、Pod、Endpoint、NetworkPolicy 和密码初始化，而不是只查 API 主容器。

### 45.6 Running 但 0/1 Ready

说明主进程运行但 readiness 失败。describe 看 probe 错误，Pod 内/Service 路径 curl，检查应用可用状态。不要用删除探针作为永久修复。

### 45.7 Service 无法访问

看 Service selector、EndpointSlice、Pod readiness、targetPort 名、NetworkPolicy。ClusterIP 只在集群内，宿主浏览器不能直接访问。

### 45.8 Ingress 404/502

404 可能 Host/path 不匹配或 Controller 默认后端；502 可能 Service/Endpoint/端口/NetworkPolicy；还要看 IngressClass、Controller Pod、Events 和 webhook。修改本机 hosts 只解决域名解析，不会安装 Controller。

### 45.9 PVC Pending

查看 StorageClass、provisioner、容量和 access mode。删除 Pod不会解决 StorageClass 缺失。Minikube 重建后本地存储可能变化，重要数据不能只依赖实验集群。

### 45.10 HPA unknown

检查 Metrics Server、APIService、`kubectl top`、Pod requests 和 HPA events。没有指标时 HPA 不能计算，双副本仍由 Deployment 保持。

### 45.11 Rollout 卡住

看新 ReplicaSet Pod 的 Pending、pull、probe 和资源；旧 Pod是否因 maxUnavailable=0 保留；PDB主要影响驱逐，不要混淆。发布门禁超时后根据影响回滚，并保留失败 Pod/日志。

<a id="chapter-46"></a>

## 第 46 章　Kubernetes 实验与个人掌握标准

### 46.1 必做实验

1. 从零部署一次，不只运行脚本，记录每阶段对象。
2. 删除一个 API Pod，观察新名称/IP与副本恢复，验证 Service 地址不变。
3. 查看 initContainer Completed 和日志。
4. 修改镜像版本执行滚动升级，观察旧/新 ReplicaSet。
5. 执行 Helm history 和一次回滚。
6. 运行业务 smoke-test，证明账户、转账、重放和流水。
7. 验证 operator 能/不能做的三项权限。
8. 观察 PVC 与 MySQL Pod 重建后的数据。
9. 在有 Metrics Server 时再做 HPA 实验；无则记录 unknown 边界。

### 46.2 复述模板

“Compose 用于单机复现，Kubernetes 维护多副本期望状态。OpsPilot 的 API/Web 用 Deployment，各两副本，通过 Service 稳定发现；滚动策略 maxUnavailable=0/maxSurge=1，startup/readiness/liveness 区分启动、摘流和重启。MySQL StatefulSet+PVC 只用于学习持久化，不是生产高可用；类生产 values 关闭内置库并连接外部数据库。HPA、PDB、NetworkPolicy 的效果依赖 Metrics Server、多节点和支持策略的 CNI，所以我会分别说明对象存在、实机验证和生产边界。”

### 46.3 本篇验收题

1. Pod 被删后为什么回来？谁创建的？
2. Service 为什么比 Pod IP 稳定？
3. Ingress 对象和 Ingress Controller 有什么区别？
4. readiness 失败与 liveness 失败分别发生什么？
5. initContainer 等到 3306 为什么仍不能证明数据库可用？
6. 两副本在单节点为什么不是节点高可用？
7. PVC 与备份有何区别？
8. HPA 为什么依赖 request 和 Metrics Server？
9. PDB 为什么防不了节点突然宕机？
10. NetworkPolicy 对象存在为什么不一定真正生效？
11. Helm test 与业务 smoke-test 各证明什么？
12. 数据库迁移为什么会限制应用回滚？

<a id="part-01"></a>

# 第八篇：交付、运行与企业级演进

<a id="chapter-47"></a>

## 第 47 章　四种运行方式：先知道自己正在运行哪一套

OpsPilot 不是只有一种启动方式。学习时最容易出现的混乱，是后端在 IDEA 中运行、MySQL 在 Compose 中运行、前端又访问了另一个端口，最后不知道请求究竟去了哪里。任何操作之前，都先回答四个问题：进程在哪里、配置从哪里来、端口映射是什么、日志在哪里看。

| 运行方式 | 主要目的 | Java 在哪里 | MySQL 在哪里 | 前端入口 | 适合阶段 |
|---|---|---|---|---|---|
| IDEA + Docker MySQL | 写代码、断点调试 | Windows/IDEA | Docker 容器 | Vite 开发服务器或 Nginx | Java 学习与联调 |
| 完整 Docker Compose | 一键演示闭环 | Docker 容器 | Docker 容器 | `http://localhost:18000` | 简历演示与本机验收 |
| Rocky Linux 原生服务 | 理解传统企业运维 | systemd 管理的 JVM | 原生或独立 MySQL | Nginx | 央国企/传统运维能力 |
| Kubernetes/Helm | 理解云原生编排 | Pod | StatefulSet + PVC | Service/Ingress/端口转发 | 云网运维与进阶展示 |

### 47.1 IDEA 模式的请求链

推荐把数据库单独启动，再由 IDEA 运行后端。此时请求链通常是：

```text
浏览器 -> Vite 或前端容器 -> Windows 上的 Spring Boot:18080
                                  -> Docker MySQL:3307 -> 容器内 MySQL:3306
```

`3307:3306` 的含义不是 MySQL 改成了 3307，而是宿主机用 3307，容器内部仍监听 3306。Java 在宿主机运行时连接 `localhost:3307`；Java 也在 Compose 网络中运行时连接 `mysql:3306`。把这两个地址混用，是最常见的连接失败原因之一。

观察顺序：先看 IDEA 控制台是否出现应用启动成功，再访问健康端点，再请求业务 API，最后才看前端。如果后端 API 本身不通，不要先改 CSS 或 Nginx。

### 47.2 Compose 模式的请求链

完整 Compose 模式由同一份编排文件管理 Web、应用、数据库和观测组件。浏览器只需要访问 Nginx 暴露的 18000 端口；Nginx 通过容器网络把 `/api` 转发到应用容器。

```text
Edge:18000 -> web(Nginx):80 -> app:8080 -> mysql:3306
                                |
                                +-> /actuator/prometheus <- Prometheus
容器日志 -> Alloy -> Loki -> Grafana
告警规则 -> Prometheus -> Alertmanager
```

这里的 `app:8080` 是容器网络中的服务名和内部端口，宿主机访问应用才使用 `localhost:18080`。能区分“容器内地址”和“宿主机映射地址”，是理解 Docker 网络的关键。

### 47.3 Linux 原生模式的价值

原生部署不是为了否定容器，而是让你真正理解服务管理、权限、目录、日志、端口和反向代理。很多央国企存量系统仍然以 JAR、systemd、Nginx 和数据库中间件的形式运行。学会这条路线后，再看容器会更清楚：容器实际上把文件系统、进程和网络隔离起来，但应用仍然遵循同样的启动与故障规律。

### 47.4 Kubernetes 模式的边界

Kubernetes 解决的是声明式编排、故障自愈、滚动发布、服务发现和资源调度，不会自动解决代码缺陷、慢 SQL、数据一致性或错误配置。单机 Minikube 可以证明清单和流程可运行，但不能等同于真实多节点生产集群。

### 47.5 运行方式自检卡

每次启动前写下：

1. 浏览器入口是什么；
2. 前端代理目标是什么；
3. Java 进程或容器叫什么；
4. Java 使用哪个配置文件；
5. 数据库主机名、宿主机端口和容器端口分别是什么；
6. 健康检查、指标、日志和告警从哪里看；
7. 停止命令会不会删除数据卷。

验收标准：不看文档也能画出当前运行模式的请求链，并能指出任意一段失败时先到哪里取证。

<a id="chapter-48"></a>

## 第 48 章　完整 Docker Compose 运行手册

### 48.1 启动前检查

启动不是机械执行命令。先确认 Docker Desktop 已进入 Running 状态，Linux 容器引擎可用，18000、18080、3307、13000、19090、19093 等端口没有被其他程序占用，并确认磁盘有足够空间。

```powershell
docker version
docker compose version
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 18000,18080,3307,13000,19090,19093
docker system df
```

你应该观察什么：`docker version` 同时有 Client 和 Server；端口检查没有显示未知占用；`docker system df` 没有逼近磁盘容量上限。若只有 Client 没有 Server，说明命令行存在，但 Docker 引擎没有正常工作。

### 48.2 配置展开与镜像构建

在真正创建容器前，先让 Compose 展开配置。这能提前发现 YAML、变量替换、挂载路径和依赖关系问题。

```powershell
docker compose config
docker compose build
```

`config` 成功只说明配置语法和变量解析基本正确，不代表镜像一定能拉取，也不代表应用能启动。`build` 失败时先分辨是网络下载失败、依赖仓库失败、编译测试失败，还是 Dockerfile 路径错误。

### 48.3 分层启动法

第一次或故障排查时，不要把十个容器一次启动后盲目等待。按依赖层启动更容易定位：

1. 启动 MySQL，等健康；
2. 启动应用，确认 Flyway 和健康端点；
3. 启动 Web，验证真实 API；
4. 启动 Prometheus、Grafana、Alertmanager；
5. 启动 Loki、Alloy、node-exporter、cAdvisor。

正常演示可以使用项目的本地启动脚本统一完成，但你仍应理解脚本内部做了什么：环境检查、编排启动、健康门禁、种子数据、关键入口输出。

### 48.4 启动后的五层验收

| 层次 | 检查目标 | 证据 | 不能替代什么 |
|---|---|---|---|
| 容器 | 是否处于 Running/healthy | `docker compose ps` | 不能证明业务正确 |
| 进程 | 应用是否启动完成 | 应用日志、健康端点 | 不能证明数据闭环 |
| 网络 | 端口和代理是否通 | `curl`、浏览器 Network | 不能证明事务正确 |
| 业务 | 创建账户、转账、流水 | API 返回和 MySQL 数据 | 不能证明监控完整 |
| 运维 | 指标、日志、告警是否可追踪 | Prometheus/Grafana/Loki/Alertmanager | 不能证明生产高可用 |

### 48.5 停止、清理和数据风险

普通停止优先使用 `docker compose stop` 或不带卷删除的 `down`。带 `-v` 会删除 Compose 管理的数据卷，MySQL 数据也可能随之消失。执行前必须确认目标项目和数据是否可丢弃。

安全口述：停止容器与删除卷是两个动作；测试环境可以在确认后重置数据，演示和生产环境必须先备份、核验备份可恢复，再考虑删除持久化数据。

<a id="chapter-49"></a>

## 第 49 章　Rocky Linux 原生部署全流程

本章对应“传统 Linux 运维闭环”。目标不是背命令，而是把一个 Java Web 系统从构建产物变成由操作系统托管、可重启、可看日志、可回滚的服务。

### 49.1 部署前规划

先规划账户和目录，避免所有东西都放在 `/root`：

```text
/opt/opspilot/releases/<版本>/       不可变发布目录
/opt/opspilot/current -> releases/... 当前版本软链接
/etc/opspilot/                      环境配置和密钥
/var/log/opspilot/                  应用/部署日志
/var/lib/opspilot/                  需要持久化的运行数据
opspilot                            无登录服务账户
```

原则：程序、配置、日志和数据分离；运行账户只拥有完成任务所需的最小权限；密钥不打进 JAR，也不提交到 Git；发布版本不可原地覆盖。

### 49.2 环境检查

需要检查操作系统版本、CPU 架构、内存、磁盘、时间同步、Java 版本、MySQL 连通性、DNS、端口和防火墙。环境检查要输出明确的成功或失败，而不是静默执行。

```bash
cat /etc/os-release
uname -m
free -h
df -hT
timedatectl status
java -version
ip -br addr
ip route
ss -lntp
```

时间同步很重要，因为证书有效期、日志时间线、数据库时间、监控采样和审计都会依赖它。多台机器时间不一致时，一次故障会在日志里呈现错误顺序。

### 49.3 构建产物和校验

推荐在构建环境生成 JAR 和前端静态文件，再传到目标主机，不在生产机临时下载一整套开发依赖。产物应有版本号和校验值。

```bash
sha256sum opspilot-api-<版本>.jar
```

上传后再次计算校验值。如果前后不一致，停止发布，不能抱着“也许还能运行”的心态继续。

### 49.4 MySQL 初始化

创建独立数据库和最小权限账户。应用账户通常需要对自身库进行读写和迁移所需权限，但不应拥有整个数据库实例的管理权限。密码通过受控配置注入。

上线前确认：字符集、时区、最大连接数、磁盘目录、备份计划、慢查询设置和防火墙来源范围。Flyway 会在应用启动时执行版本迁移，因此数据库权限和迁移兼容性必须在发布前验证。

### 49.5 systemd 服务

systemd 的价值是统一启动、停止、重启、开机自启、状态查询和日志关联。服务单元至少应定义工作目录、运行用户、环境文件、JVM 启动命令、失败重启策略和退出超时。

```ini
[Unit]
Description=OpsPilot API
After=network-online.target
Wants=network-online.target

[Service]
User=opspilot
Group=opspilot
WorkingDirectory=/opt/opspilot/current
EnvironmentFile=/etc/opspilot/opspilot.env
ExecStart=/usr/bin/java -jar /opt/opspilot/current/opspilot-api.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
TimeoutStopSec=30

[Install]
WantedBy=multi-user.target
```

`SuccessExitStatus=143` 表示把收到 SIGTERM 后的正常退出视作成功。停止服务时，systemd 先给进程优雅退出机会，超时后才强制终止。JVM 是否真正优雅关闭，还取决于应用和连接池能否在超时内完成收尾。

变更 unit 文件后必须重新加载 systemd 配置；启用服务不等于已经启动；状态是瞬时结果，仍需结合日志和业务验证。

### 49.6 Nginx 与静态前端

前端构建产物由 Nginx 提供，`/api/` 代理到本机应用端口。SPA 路由需要 `try_files` 回退到 `index.html`，否则直接刷新 `/accounts` 会出现 404。

代理配置还要考虑：超时、请求体大小、真实客户端地址、协议头、请求 ID、访问日志，以及只对需要的路径开放。配置修改先做语法检查，再平滑重载，不能直接杀进程。

### 49.7 部署后的健康门禁

发布脚本不应把“systemctl start 返回 0”当成成功。它应循环请求健康端点，设置最大等待时间，并在失败时输出最近日志。接着执行最小冒烟测试：查询账户列表、创建测试账户或读取既有数据、验证 Nginx 代理。

### 49.8 Linux 部署实验

实验目标：在虚拟机中完成一次全新部署和一次版本替换。

故障注入：故意把数据库密码写错。预期现象是应用启动失败或 readiness 失败；你应从 systemd 状态进入 journal，找到数据库认证失败，而不是反复重启。修复配置后重启，再用健康端点和业务请求验收。

复述要求：说明为什么使用服务账户、为什么程序和配置分离、为什么发布目录不可变、为什么启动成功后还要做健康门禁。

<a id="chapter-50"></a>

## 第 50 章　自动化脚本：把人工步骤变成受控流程

自动化不是“把命令连起来”这么简单。可靠脚本需要幂等、明确输入、失败即停、日志、检查点、超时和回滚提示。

### 50.1 脚本的六项基本约束

1. **明确目标**：脚本开头说明影响的环境和组件；
2. **输入校验**：路径、端口、版本号和必需变量不存在时立即失败；
3. **幂等性**：重复运行不应无限追加配置或制造重复数据；
4. **失败可见**：失败时返回非零退出码并指出哪一步失败；
5. **安全边界**：删除、覆盖、卷清理前验证精确目标；
6. **验收输出**：最后给出健康、版本、入口和日志位置。

### 50.2 环境检查脚本应检查什么

环境检查不修改系统，只收集事实：系统版本、CPU/内存/磁盘、Java、Docker、Compose、端口、DNS、数据库连通性、必要目录和权限。输出应可直接附在故障单中。

### 50.3 部署脚本的阶段

```text
参数校验 -> 获取/校验产物 -> 创建版本目录 -> 安装配置
-> 切换 current -> 重启服务 -> 健康门禁 -> 冒烟测试 -> 记录发布结果
```

每一步都应能回答“失败后系统处于什么状态”。如果软链接已经切换但服务启动失败，脚本需要保留旧版本信息，便于回滚。

### 50.4 常见脚本反模式

- 用固定等待 30 秒代替健康轮询；
- 把数据库密码直接打印到日志；
- 使用宽泛通配符删除目录；
- 无条件覆盖配置文件；
- 忽略子命令退出码；
- 在生产机在线编译；
- 只有“成功”字样，没有版本和验收证据。

### 50.5 验收

同一部署脚本连续执行两次，第二次应安全完成或明确提示目标版本已经部署。故意制造端口占用或错误密码，脚本应在门禁阶段失败，并留下足够信息定位原因。

<a id="chapter-51"></a>

## 第 51 章　发布、健康门禁与回滚

### 51.1 什么是一次完整发布

一次发布不是把文件传上去，而是：确定变更范围、构建测试、产物留痕、备份或兼容性确认、部署、健康检查、业务冒烟、观测指标、决定继续或回滚、记录结果。

### 51.2 不可变发布

每个版本放在独立目录，通过 `current` 软链接指向当前版本。优点是旧产物仍在，回滚只需把链接切回并重启；也能清楚回答线上运行的具体版本。缺点是要管理磁盘保留策略，不能无限保存。

### 51.3 健康检查的三层含义

- 进程存活：JVM 是否仍运行；
- 应用可服务：Spring 容器是否启动并能接受请求；
- 关键依赖可用：数据库等是否满足当前业务需求。

OpsPilot 当前配置对 Kubernetes 探针提供 Actuator 入口，但是否把数据库纳入 readiness，必须依据实际健康组配置和端点返回验证，不能只依据代码注释宣称。健康检查过重也可能造成依赖抖动时大量 Pod 被摘除，因此生产设计需要谨慎选择检查项。

### 51.4 数据库迁移与回滚难题

代码回滚容易，数据库回滚更难。Flyway `V1` 一旦在环境执行，就不能随意修改，否则校验会失败。后续变更必须使用 `V2`、`V3`。数据库迁移应尽量向后兼容，例如先新增可空列、让新旧代码都能工作，再逐步填充和收紧约束。

危险场景：新版本删除旧版本依赖的列，代码发布失败后即使切回旧 JAR，旧代码也不能工作。企业发布通常采用“扩展—迁移—收缩”策略，而不是一次性破坏性修改。

### 51.5 回滚触发条件

提前定义阈值，不要靠现场感觉。例如：健康门禁超时、错误率持续升高、核心转账接口失败、数据库迁移失败、关键页面不可用。回滚后仍要验证数据是否受到影响；回滚程序不会自动撤销已经执行的业务数据和数据库结构。

### 51.6 Kubernetes 发布与回滚

Deployment 滚动更新会逐步创建新 Pod、替换旧 Pod。readiness 未通过的新 Pod 不应接收流量。`rollout status` 观察发布进度，`rollout history` 查看历史，回滚后还要重新做冒烟测试。若镜像使用可变 `latest` 标签，版本证据和回滚可靠性都会变差，因此应使用不可变版本标签或镜像摘要。

<a id="chapter-52"></a>

## 第 52 章　备份、恢复、RPO 与 RTO

### 52.1 备份不是复制一个文件

备份目标是发生故障后能够恢复业务所需数据。只执行导出而从未恢复验证，不能证明备份可用。数据库备份还需要记录版本、字符集、时区、账号权限、校验值、生成时间和保留周期。

### 52.2 RPO 与 RTO

- RPO（恢复点目标）：最多能接受丢失多长时间的数据；
- RTO（恢复时间目标）：故障后多长时间内恢复服务。

每天一次全量备份意味着理论上可能丢失接近一天的数据；是否可接受取决于业务。OpsPilot 学习环境可以设定练习目标，例如 RPO 24 小时、RTO 60 分钟，但不能把这个练习数值包装成真实金融生产标准。

### 52.3 逻辑备份与恢复演练

MySQL 可使用逻辑导出保存表结构和数据。演练过程应是：生成备份、计算校验、创建一个独立恢复库、导入、核对表数量和关键记录、运行只读业务查询、记录耗时。不要直接拿唯一生产库做破坏性恢复实验。

### 52.4 容器卷的误区

Docker volume 提供持久化，不等于备份。卷仍可能被误删、磁盘损坏或数据逻辑性破坏。备份必须独立于正在运行的数据副本，并按保留策略存放到不同故障域。

### 52.5 恢复验收

恢复后至少检查：Flyway 版本表、账户数量、余额汇总、订单和流水关联、最近数据时间、应用查询、日志中是否有兼容错误。对 OpsPilot 还可以执行账务平衡查询，确保每笔成功转账仍有两条方向相反、金额一致的流水。

<a id="chapter-53"></a>

## 第 53 章　故障演练与事件处理

### 53.1 为什么要主动制造故障

只会在正常状态启动项目，不足以证明运维能力。可控故障演练让你练习发现、定位、缓解、恢复和复盘。所有演练都要限定环境、精确目标、恢复步骤和成功标准。

### 53.2 标准事件流程

```text
发现 -> 定级 -> 止损 -> 取证 -> 定位 -> 修复/回滚
-> 验证 -> 恢复观察 -> 复盘 -> 改进项闭环
```

先止损不代表跳过证据。比如应用持续写入错误数据时，可能需要先阻断写请求，但同时应保存日志、指标、版本和变更记录。

### 53.3 六个建议演练

| 演练 | 注入方式 | 预期现象 | 主要证据 | 恢复 |
|---|---|---|---|---|
| 数据库停止 | 停止测试 MySQL | API 失败/readiness 变化 | 应用日志、连接池、健康端点 | 启动 DB，观察重连 |
| 应用停止 | 停止 app 容器 | Nginx 502，Prometheus target down | Nginx 日志、Targets | 重启 app |
| 密码错误 | 修改测试配置 | 认证失败 | 启动日志、MySQL 日志 | 恢复配置 |
| 端口占用 | 启动占位监听 | 服务绑定失败 | systemd/应用日志、`ss` | 释放或改端口 |
| 磁盘压力 | 在专用测试目录制造有限文件 | 日志/数据库写入风险 | `df`、告警 | 删除测试文件 |
| 镜像错误 | 部署不存在的标签 | ImagePullBackOff | Pod events | 修正镜像或加载本地镜像 |

磁盘演练必须在专门测试环境使用严格大小限制，绝不能填满系统盘。删除时验证绝对路径，避免宽泛递归命令。

### 53.4 五问复盘

1. 用户看到了什么影响；
2. 最早的可观察信号是什么；
3. 为什么现有防线没有提前阻止；
4. 哪个动作恢复了服务，证据是什么；
5. 如何通过代码、配置、告警、手册或演练防止复发。

复盘针对系统和流程，不针对个人。改进项要有负责人、截止时间和验收条件。

<a id="chapter-54"></a>

## 第 54 章　安全：当前项目做了什么，还缺什么

安全部分必须区分“已有实践”和“生产必需但尚未实现”。如实说明边界，比罗列安全名词更可信。

### 54.1 已有的安全基础

- 容器尽量使用非 root 用户；
- 可配置只读根文件系统和临时目录；
- Kubernetes 使用 RBAC 和 NetworkPolicy 示例；
- 数据库使用独立账号和环境变量注入；
- 接口有参数校验、事务和幂等约束；
- 日志具有关联 ID，便于审计链路；
- Docker/Kubernetes 都设置健康检查和资源约束示例。

这些措施降低了部分运行风险，但不能构成完整的身份与访问控制。

### 54.2 当前明确缺失

OpsPilot 当前没有完整的登录认证、角色授权、TLS 证书终止、WAF、密钥管理平台、数据库传输加密、依赖漏洞门禁、镜像签名、审计防篡改和数据脱敏机制。转账接口在受信任的学习环境可演示，但不能直接暴露到公网。

### 54.3 身份认证与授权

生产演进可引入 Spring Security，至少区分运维只读、业务操作和管理员角色。认证回答“你是谁”，授权回答“你能做什么”。仅有登录页面但后端没有权限校验，是伪安全。

### 54.4 密钥和配置

环境变量比硬编码更好，但并非最终密钥管理。生产可使用 Kubernetes Secret 配合外部密钥系统，并控制谁能读取。Secret 默认只是 Base64 编码，不等于加密。日志、进程参数、错误页面和前端构建产物都不应泄露密码。

### 54.5 网络边界

公网只开放必要入口；数据库不直接暴露公网；Nginx/Ingress 终止 TLS；安全组、防火墙和 NetworkPolicy 分层限制来源。NetworkPolicy 能否生效取决于 CNI 实现，写了 YAML 不等于策略真的执行。

### 54.6 供应链安全

依赖库和基础镜像会产生漏洞风险。企业化流程需要固定版本、漏洞扫描、SBOM、镜像仓库权限、签名验证和定期升级。升级不能只看“最新”，还要做兼容测试和回滚准备。

<a id="chapter-55"></a>

## 第 55 章　企业级差距与合理演进路线

### 55.1 为什么现在看起来像技术栈堆砌

如果简历只写“使用 Docker、Kubernetes、Prometheus、Grafana、Loki、MySQL、React”，确实像堆砌。真正的项目主线应该只有一条：一个有事务要求的转账业务，如何被可靠地开发、部署、观察、告警、恢复和演进。

每项技术必须回答它解决了哪类问题：

- MySQL 事务保证一次转账的数据原子性；
- 幂等键避免请求重试制造重复业务；
- Docker 固化运行环境并完成多服务编排；
- Kubernetes 管理副本、发布、自愈和资源；
- Prometheus 发现趋势和异常，Loki 提供日志证据；
- Alertmanager 对告警去重、分组和静默；
- systemd/Nginx 展示传统 Linux 服务交付；
- 自动化脚本降低重复人工操作的不一致。

只要讲述始终围绕问题与证据，技术就不是装饰。

### 55.2 当前项目完成度矩阵

| 能力 | 当前状态 | 可展示证据 | 不能过度宣称 |
|---|---|---|---|
| 业务闭环 | 已实现 | 账户、转账、订单、双流水、看板 | 不是银行核心系统 |
| 数据一致性 | 已实现基础机制 | 事务、锁、幂等、测试 | 未做高并发压测 |
| 容器交付 | 已实现 | Compose、多阶段镜像、健康检查 | 非生产集群 |
| Linux 部署 | 已有脚本/文档路线 | systemd、Nginx、检查与回滚 | 需本人重新实操留证 |
| 观测闭环 | 已实现本地栈 | 指标、日志、告警页面 | 无真实外部通知与长期 SLO |
| Kubernetes | 已实现实验清单与 Helm | 副本、探针、HPA/PDB、RBAC、策略 | 单机 Minikube 不等于 HA |
| CI/CD | 设计空间 | 发布脚本与门禁思路 | 当前没有完整流水线证据 |
| 安全 | 有基础样例 | 非 root、RBAC、NetworkPolicy | 无认证授权/TLS/密钥平台 |

### 55.3 第一阶段：把现有功能真正掌握

先不再加新组件。独立完成启动、业务请求、数据库查询、日志定位、一个告警实验、一次容器故障、一次 Pod 故障、一次发布回滚。能解释代码关键路径并修改一个小功能。这个阶段完成后，现有项目才真正属于你。

### 55.4 第二阶段：补最有价值的工程缺口

优先级建议：

1. 增加 Git 提交历史和版本标签；
2. 增加 GitHub Actions 或企业常见流水线，执行测试、构建和镜像扫描；
3. 增加 OpenAPI 文档和统一认证授权；
4. 增加并发转账测试与基础压力测试；
5. 把告警接入真实邮件或测试 Webhook；
6. 建立固定的故障演练记录和恢复时间数据。

### 55.5 第三阶段：按岗位选方向，不无限扩张

面向网络/系统运维：加深 Linux、DNS、路由、防火墙、Nginx、抓包、备份恢复和容量分析。

面向云网运维：加深 Kubernetes 多节点、Ingress、CNI、存储类、监控 Operator、Helm 与发布策略。

面向运维开发/银行科技：加深 Java 事务、并发、接口安全、自动化平台、流水线和数据库性能。

不要三条同时冲到生产深度。主方向深入，另外两条保持能解释和能演示即可。

### 55.6 是否需要 Milvus

当前项目不需要 Milvus。Milvus 是向量数据库，适合语义检索、推荐或 RAG 场景；OpsPilot 的核心问题是交易一致性和运维闭环。为了“显得高级”接入 Milvus会增加无关复杂度，反而削弱项目主线。将来若新增“基于历史故障手册的智能排障助手”，才有合理使用向量检索的场景，而且还必须处理证据引用、知识更新、权限与拒答。

### 55.7 AI/Codex 的参与如何表述

AI 可以参与需求拆分、脚手架生成、代码审查、测试补充、文档整理和排障建议，但简历的技术能力仍然必须由你负责验证。推荐表述为“使用 AI 编程工具辅助方案分析、测试生成与文档沉淀，并通过代码审查、自动化测试和环境验收确认结果”。

不能表述成“熟练掌握某技术”仅因为 AI 写过对应文件。面试中你要能解释关键代码、复现启动、制造故障、找到证据、说明修复和边界。AI 是效率工具，不是能力所有权的替代品。

<a id="chapter-56"></a>

## 第 56 章　OpsPilot 的完整闭环总结

OpsPilot 的完整闭环可以从七条线理解：

1. **业务闭环**：账户创建—转账—订单—双流水—看板；
2. **数据闭环**：校验—锁定—事务—幂等—异常回滚—查询核验；
3. **交付闭环**：构建—镜像/产物—配置—部署—健康门禁—冒烟；
4. **观测闭环**：指标—日志—面板—规则—告警—关联 ID 定位；
5. **恢复闭环**：故障发现—止损—修复/回滚—业务验证—复盘；
6. **平台闭环**：Linux 原生—Compose—Kubernetes—Helm；
7. **学习闭环**：理解原理—亲手操作—制造失败—解释证据—独立复述。

你在面试中不需要把所有名词一口气说完。先讲业务问题，再讲最关键的可靠性设计，然后用一次可复现的故障排查证明运维能力，最后诚实说明尚未生产化的边界。这比堆叠二十个工具名称更像真实项目。

<a id="part-01"></a>

# 第九篇：把项目真正学成自己的能力

<a id="chapter-57"></a>

## 第 57 章　从你当前基础出发的学习地图

你目前刚系统学完 Linux 基础命令，了解 Docker 基本功能，正在进入计算机网络学习，Java 有学习经历但需要恢复独立编码能力。这个起点可以完成 OpsPilot，但不适合一开始同时深挖所有组件。正确方式是按依赖关系逐层掌握，并把每一层都落到项目现象。

### 57.1 能力分层

| 层级 | 标准 | 你应达到的项目证据 |
|---|---|---|
| L0 见过 | 知道名词和用途 | 能在架构图中指出位置 |
| L1 会用 | 能按步骤完成正常操作 | 能独立启动、访问、停止 |
| L2 会查 | 失败时知道去哪里取证 | 能从状态、日志、端口、配置定位 |
| L3 会改 | 能改一个小功能并验证 | 能改接口/面板/规则/配置并测试 |
| L4 会设计 | 能解释取舍和边界 | 能回答为什么这样做、何时不适用 |

秋招项目的最低目标不是所有技术都到 L4。建议 Linux、网络、Docker 达到 L3；Java/MySQL 关键业务达到 L2-L3；Prometheus/Grafana 达到 L2；Kubernetes 达到 L1-L2，并对生产边界有 L3 的诚实判断。

### 57.2 八周主路线

#### 第 1 周：跑通与画图

目标：独立完成 Compose 启停，走完创建账户和转账，画出请求链。

每天成果：一张手绘链路图、一份端口表、一次完整业务录屏或截图、一篇启动失败记录。不要研究 HPA 和 RBAC。

验收：关闭文档后，能说明浏览器请求如何经过 Nginx、Spring Boot 和 MySQL；能解释 18080 与容器内 8080 的区别。

#### 第 2 周：Linux 与网络定位

目标：在 Rocky Linux 虚拟机中识别进程、端口、服务、日志、路由和 DNS。

成果：部署环境检查记录；完成端口占用、DNS 错误、Nginx 502 三个故障；每次记录现象、证据、根因、修复和验证。

验收：面对“网页打不开”，不会直接重启全部服务，而能按 DNS—TCP—代理—应用—数据库逐段定位。

#### 第 3 周：Docker 与 Compose

目标：理解镜像、容器、网络、卷、健康检查和多阶段构建。

成果：自己解释两个 Dockerfile；重新构建镜像；查看容器内部进程、挂载和网络；完成停止数据库、错误环境变量、Nginx 代理失败三个实验。

验收：能说出 Running 与 healthy 的区别，以及为什么 healthy 仍不必然等于业务正确。

#### 第 4 周：Java 请求链

目标：从一个 HTTP 请求读通 Filter—Controller—Service—Repository—MySQL。

成果：独立增加一个只读接口或查询条件；写 DTO 校验；补一个集成测试；用断点观察一次转账。

验收：能解释依赖注入、分层、状态码、异常映射、事务边界和关联 ID。

#### 第 5 周：MySQL、事务与幂等

目标：理解三张核心表、索引、事务、锁、幂等键和账务平衡。

成果：写出关键 SQL；验证成功转账的两条流水；故意制造余额不足并证明没有部分更新；重复同一幂等键并验证不重复扣款。

验收：能在白板上按顺序讲出转账服务每一步，并指出并发测试目前的不足。

#### 第 6 周：监控、日志和告警

目标：能区分指标、日志、健康检查和告警管理。

成果：在 Prometheus 查一个 JVM 指标和一个 HTTP 指标；在 Grafana 找到趋势；在 Loki 用关联 ID 查日志；制造服务停止，观察 Target 和告警状态。

验收：能解释 Prometheus 不是接口文档，Alertmanager 也不负责采集指标。

#### 第 7 周：Kubernetes

目标：在 Minikube 中部署并理解 Deployment、Service、StatefulSet、PVC 和探针。

成果：查看 Pod、日志、事件；删除一个应用 Pod 观察自愈；做一次错误镜像演练；执行一次 Helm 安装或升级。

验收：能区分 Service 与 Ingress、liveness 与 readiness、requests 与 limits，并说明单机实验边界。

#### 第 8 周：发布、演示与面试

目标：形成可复现证据，做完整演示，接受追问。

成果：Git 提交和版本标签；一份验收报告；一次发布回滚；一份故障复盘；三种时长项目讲述；模拟面试题录音。

验收：演示中即使某组件失败，也能用证据说明问题并切换备用展示，不慌乱堆命令。

### 57.3 学习时间不够时的裁剪

必须保留：Linux、网络链路、Docker Compose、Java 转账请求链、MySQL 事务与幂等、Prometheus/Grafana 基础、一次 K8s 部署和一次故障。

可以暂缓：多集群、Service Mesh、复杂 Operator、分布式链路追踪、Milvus、消息队列、数据库高可用集群。它们并非无价值，只是与当前主线和时间投入不匹配。

<a id="chapter-58"></a>

## 第 58 章　统一学习法：原理—观察—操作—故障—复述—验收

每个知识点都使用同一模板，避免“命令执行过但没有学会”。

### 58.1 原理

用自己的话回答它解决什么问题、位于哪一层、依赖谁、失败会影响谁。例如 Service 解决 Pod 地址变化下的稳定访问，依赖标签选择器找到后端 Pod。

### 58.2 观察

先观察正常系统，而不是一上来改配置。找到状态、日志、指标、数据和端口证据。记录一份正常基线，故障时才知道什么发生了变化。

### 58.3 操作

执行最小且可撤销的动作。一次只改一个变量，记录操作时间。若同时改三个配置，即使恢复了，也难以证明真正根因。

### 58.4 故障

主动制造一种安全故障，提前写恢复方法。观察错误从哪一层暴露：浏览器、Nginx、Java、MySQL、Prometheus 还是 Kubernetes Event。

### 58.5 复述

不用术语堆砌，按“现象—链路—证据—根因—修复—预防”讲 1 分钟。能说清楚才算形成可面试能力。

### 58.6 验收

使用可判定标准：状态码、记录数量、指标值、日志字段、恢复耗时、Pod 状态，而不是“看起来正常”。

### 58.7 示例：学习 readiness

原理：readiness 表示当前实例是否应接收流量。

观察：查看探针配置、端点返回、Pod Ready 条件和 Service Endpoints。

操作：正常状态请求 readiness，记录返回；查看 Pod 描述。

故障：在测试环境使应用无法连接关键依赖，观察 readiness 是否真的变化。若不变化，说明当前健康组可能未纳入该依赖。

复述：readiness 失败不必杀死容器，而是把实例从服务流量中摘除；liveness 用于判断是否需要重启，两者不能混用。

验收：能用实际端点和 Pod 条件证明，而不是引用注释。

<a id="chapter-59"></a>

## 第 59 章　24 个循序渐进实训任务

#### 实训 1：创建系统基线

目的：知道项目正常时是什么样子。

操作：记录容器列表、端口、健康端点、首页、账户数量、Prometheus Targets、Grafana 数据源和最近日志。

故障：无。

产物：`baseline-日期.md`，包含时间、版本、命令输出摘要和截图。

验收：下一次故障可以与基线比较。

#### 实训 2：画出端口与网络表

目的：消除宿主机端口和容器端口混淆。

操作：整理入口、宿主机端口、容器端口、协议、调用方、被调用方。

故障：把应用地址错误地写成宿主机端口，观察容器间调用失败。

验收：能解释 `18080:8080`。

#### 实训 3：进程与端口对应

目的：把 Linux 进程、Socket 和服务联系起来。

操作：在 Linux 查询监听端口和 PID，再查看进程命令、父进程、运行用户和 systemd 单元。

故障：启动第二个相同端口的进程。

验收：从“地址已被占用”定位到具体 PID，并安全处理正确进程。

#### 实训 4：systemd 服务管理

目的：掌握企业常见 Java 服务托管。

操作：安装 unit、重载、启用、启动、查看状态和 journal。

故障：写错 JAR 路径。

验收：从退出码和日志定位，不通过盲目重装 Java 解决。

#### 实训 5：Nginx 反向代理

目的：理解代理层与后端层的边界。

操作：配置静态前端和 `/api` 代理，检查配置并平滑重载。

故障：把 upstream 端口改错。

验收：解释 502 的含义，并证明后端端口恢复后代理正常。

#### 实训 6：DNS 与连通性

目的：学会逐层验证网络。

操作：分别使用域名和 IP 测试解析、TCP 连接和 HTTP。

故障：在实验环境配置错误域名。

验收：能区分 DNS 失败、TCP 超时、连接拒绝和 HTTP 500。

#### 实训 7：容器生命周期

目的：理解镜像与容器不是同一个对象。

操作：从同一镜像创建两个容器，查看其 ID、网络和可写层。

故障：让主进程退出。

验收：说明容器为何停止、重启策略何时生效。

#### 实训 8：数据卷持久化

目的：理解容器删除与数据保留。

操作：创建测试记录，重建 MySQL 容器，确认卷仍保留数据。

故障：仅在已备份的练习环境演示删除卷。

验收：说清 volume 不是备份，并完成独立恢复。

#### 实训 9：多阶段构建

目的：理解构建环境与运行环境分离。

操作：阅读 Dockerfile 两个阶段，比较依赖和最终镜像内容。

故障：在构建阶段删除必要文件或写错复制路径。

验收：解释为什么最终镜像不需要 Maven/Node 全套工具。

#### 实训 10：Compose 服务发现

目的：掌握容器间使用服务名通信。

操作：在 app 容器中解析并连接 `mysql:3306`。

故障：把地址改成 `localhost:3307`。

验收：解释容器里的 localhost 指向谁。

#### 实训 11：读通一个 REST 请求

目的：连接前端、HTTP、Java 分层和数据库。

操作：浏览器发请求，记录 Network；后端断点进入 Controller、Service、Repository；查看 SQL 和返回。

故障：传入非法金额。

验收：能解释 4xx 与 5xx 的责任差异。

#### 实训 12：增加一个查询功能

目的：恢复独立 Java 开发。

操作：为流水增加按账户筛选或分页参数，完成 DTO、服务、仓储和测试。

故障：传入不存在账户。

验收：测试通过，错误返回一致，前端或 curl 可调用。

#### 实训 13：事务回滚

目的：看到原子性，而不是只背 ACID。

操作：记录两账户余额；制造余额不足；再次查询余额、订单和流水。

验收：失败转账没有出现只扣不加或不完整流水。

#### 实训 14：幂等重放

目的：理解网络重试和业务重复的区别。

操作：两次提交相同幂等键与相同请求。

故障：同一幂等键提交不同业务参数。

验收：相同请求复用结果，不重复扣款；冲突请求被拒绝。

#### 实训 15：锁顺序与并发

目的：理解为什么按账户 ID 排序加锁。

操作：写两个线程同时做 A→B 与 B→A；记录成功、失败、余额和流水。

验收：没有余额破坏；若出现死锁，能从数据库或应用日志取证并解释重试策略。当前项目若尚未包含该测试，应把它作为新增成果，而不是宣称已经完成。

#### 实训 16：Flyway 迁移

目的：理解数据库结构版本化。

操作：保留已执行的 V1，新建 V2 增加一个向后兼容字段，在空库和已有 V1 库分别验证。

故障：尝试修改 V1 并观察校验错误，仅用于理解后立即恢复。

验收：说明为什么已执行迁移不可随意修改。

#### 实训 17：Prometheus 查询

目的：知道指标如何产生和被采集。

操作：从 Actuator 原始指标到 Prometheus Targets，再执行 JVM/HTTP 查询。

故障：停止 app。

验收：能解释 target down 与业务错误率升高不是同一件事。

#### 实训 18：Grafana 面板

目的：把 PromQL 结果变成趋势视图。

操作：确认数据源，打开仪表盘，制造一段请求流量。

故障：选择错误时间范围。

验收：能从面板下钻到原始查询，并说明“无数据”的常见原因。

#### 实训 19：关联 ID 日志追踪

目的：从一次前端错误定位到后端日志。

操作：记录响应中的关联 ID，在 Loki 或应用日志中检索同一 ID。

故障：发送非法或失败请求。

验收：能串起请求入口、异常和状态码。

#### 实训 20：告警生命周期

目的：理解 Prometheus 与 Alertmanager 的职责分工。

操作：制造一个安全且可恢复的告警，观察 Pending、Firing、Resolved；在 Alertmanager 查看分组和静默。

验收：说明当前项目没有外部通知接收器时，UI 中出现告警不等于手机会收到消息。

#### 实训 21：Kubernetes 自愈

目的：理解期望状态控制。

操作：记录 Deployment 期望副本，删除一个 Pod，观察新 Pod 创建、探针通过和 Service 端点恢复。

验收：说明是控制器重建 Pod，不是被删除的 Pod 自己复活。

#### 实训 22：ImagePullBackOff

目的：学会以 Event 为第一证据。

操作：部署错误镜像标签，查看 Pod 状态、describe 和 Events。

恢复：修正标签，或在 Docker Hub 不可用时把本地镜像加载进 Minikube。

验收：能区分镜像拉取失败、容器启动失败和探针失败。

#### 实训 23：发布与回滚

目的：形成版本交付闭环。

操作：发布带明显版本标识的小改动，执行健康门禁；再部署故障版本并回滚。

验收：能出示旧/新版本、发布记录、失败证据、回滚动作和恢复验证。

#### 实训 24：备份恢复与最终演示

目的：验证数据可恢复，并串联全项目。

操作：备份数据库到独立文件，在独立恢复库导入，校验账务数据；随后按第 60 章脚本完成演示。

验收：记录 RPO/RTO 练习值、恢复耗时和数据核对结果。

<a id="chapter-60"></a>

## 第 60 章　20 分钟项目演示脚本

演示要有主线和备用方案。不要在面试官面前临时探索按钮，也不要花十分钟等镜像下载。

### 60.1 演示前一天

确认 Docker/Minikube状态、镜像、数据、端口和浏览器书签；保存一套正常截图和一段短录屏作为网络或环境失败的备用证据；准备一页架构图和一页边界说明。不要为了画面漂亮隐藏所有日志，运维项目需要可观察证据。

### 60.2 第 0—2 分钟：项目定位

口述：OpsPilot 是一个以账户转账为业务载体的运维与开发一体化项目。重点不是模拟完整银行，而是展示一个服务如何保证基础数据一致性，并完成容器化部署、指标日志告警、发布回滚和 Kubernetes 编排。

展示：架构总图和关键技术职责表。

### 60.3 第 2—6 分钟：业务闭环

打开前端看板，创建或选择两个账户，发起转账，展示结果、订单和两条流水。回到数据库查询或 API 结果，指出同一笔转账的业务 ID、幂等键和双向流水。

一定要说明：演示金额为测试数据；余额和成功率看板的统计口径有限；项目不宣称达到银行核心系统标准。

### 60.4 第 6—10 分钟：代码与数据一致性

只打开一条关键代码路径：Controller 到 TransferService。讲六点：输入校验、幂等快速查询、账户有序锁、锁后再次检查、单事务更新、订单与双流水。

展示两个测试：重复请求不重复扣款，余额不足回滚。不要逐行读代码，用流程和不变量讲。

### 60.5 第 10—14 分钟：运行与观测

展示 Compose 服务状态、Prometheus Targets、Grafana 面板、Loki 日志查询和 Alertmanager 页面。解释四者不同职责。

用一个关联 ID 从浏览器响应查到后端日志。若时间允许停止 app 容器，展示 Nginx/Prometheus/告警的变化并恢复。

### 60.6 第 14—17 分钟：Kubernetes

展示 Deployment、Service、Pod、StatefulSet/PVC 和探针状态；删除一个应用 Pod 展示自愈。说明 HPA 依赖 metrics-server，单机 Minikube 只是功能验证，不是多节点高可用证明。

### 60.7 第 17—20 分钟：交付与边界

展示 Linux 部署/回滚脚本或一次发布记录；给出当前缺口：认证授权、CI/CD、并发压测、外部告警通知、真实多节点与数据库高可用。最后说下一步只选一个岗位方向深入，而不是继续无目的加组件。

### 60.8 演示失败时怎么处理

页面打不开：先说明正在检查入口链路，依次看端口、Nginx、app、数据库，边查边解释；若 2 分钟不能恢复，切正常截图/录屏并展示已有验收报告。诚实说明现场环境问题和已有证据，比反复点击刷新更专业。

<a id="chapter-61"></a>

## 第 61 章　三种时长的项目讲述

### 61.1 30 秒版本

OpsPilot 是我面向系统运维、云网运维和运维开发岗位完成的全栈运维项目。我用 Spring Boot、MySQL 和 React 实现账户转账闭环，通过事务、幂等键和有序加锁保证基础一致性；使用 Docker Compose 和 Kubernetes 完成部署，接入 Prometheus、Grafana、Loki、Alertmanager，并编写 Linux 部署、健康检查、回滚和故障演练脚本。项目重点是把开发、部署、观测和恢复串成一条可演示链路。

### 61.2 两分钟版本

先讲背景：很多简历项目只完成增删改查，我希望展示一个系统上线后如何被运行和排障，所以选择转账业务作为载体。

再讲核心：后端将账户更新、订单和双流水放在同一事务中，用幂等键处理客户端重试，并按账户 ID 有序获取悲观锁以降低并发死锁风险。测试使用真实 MySQL 容器验证重放、回滚和冲突场景。

然后讲交付：本地通过 Compose 运行 Web、应用、MySQL 和观测栈；Linux 路线使用 systemd 和 Nginx；Kubernetes 路线使用 Deployment、Service、StatefulSet/PVC、探针、HPA/PDB、RBAC、NetworkPolicy 和 Helm。

最后讲运维：Prometheus 采集指标，Grafana 展示趋势，Alloy/Loki 聚合日志，Alertmanager 管理告警。我做了应用停止、数据库不可用、错误镜像等演练，并使用健康门禁和回滚恢复。当前边界是还没有生产级认证、真实多节点 HA 和完整压测，这些是下一阶段计划。

### 61.3 五分钟版本的结构

1. 为什么做：岗位要求和个人能力目标；
2. 做了什么：业务不变量与架构；
3. 最难点：事务、幂等、锁顺序；
4. 怎么运行：Compose/Linux/K8s；
5. 怎么发现故障：指标、日志、告警、关联 ID；
6. 怎么恢复：探针、自愈、回滚、备份；
7. 学到了什么：跨层定位和证据意识；
8. 还缺什么：诚实边界和下一步。

<a id="chapter-62"></a>

## 第 62 章　项目面试追问题库（基础到进阶）

以下答案是理解骨架，不能逐字背诵。你应结合自己的实际操作证据补充。

### 62.1 项目与架构

**1. 为什么选择转账而不是普通博客？**  转账天然包含事务、幂等、并发、审计和故障恢复问题，能把开发与运维串起来；项目并不模拟完整银行系统。

**2. 为什么是模块化单体而不是微服务？**  当前规模下单体更易理解、测试和部署，事务边界也清晰。通过业务包划分保留模块边界，等出现独立扩缩容、团队边界或发布频率需求时再考虑拆分。

**3. 项目的核心不变量是什么？**  转账成功时源账户扣款、目标账户加款、订单成功和两条流水必须一起提交；同一幂等请求不能重复改变余额；失败不能留下部分更新。

**4. 前后端如何联调？**  浏览器请求前端入口，开发时由 Vite 代理、部署时由 Nginx 代理 `/api` 到 Spring Boot；通过 Network 状态码、后端关联 ID 和日志定位。

**5. 为什么不加 Milvus/消息队列？**  当前主线不需要语义检索或异步解耦，强行加入会增加无关复杂度。只有出现明确业务问题且收益超过运维成本时才引入。

### 62.2 Linux 与网络

**6. 网页打不开怎么查？**  先确认 URL/端口，再查 DNS、路由、TCP、TLS/HTTP、Nginx、后端进程、数据库依赖；每一步用解析结果、连接状态、状态码和日志取证。

**7. 连接超时与连接拒绝有什么区别？**  拒绝通常表示目标可达但对应端口没有监听或主动拒绝；超时可能是路由、防火墙、安全组或网络丢包，需结合路径取证。

**8. 502 和 504 通常代表什么？**  502 是代理从上游得到无效响应或无法连接；504 是等待上游超时。都应继续查 upstream 地址、进程、端口、日志和依赖。

**9. systemd 为什么比手工 `java -jar` 好？**  提供统一生命周期、开机自启、运行用户、重启策略、状态和日志关联，适合标准化运维。

**10. 如何判断端口被谁占用？**  查看监听 Socket 对应 PID，再核对进程命令、用户和服务单元，确认目标后处理，不能看到 PID 就随意结束。

**11. 为什么关注时间同步？**  日志关联、证书、数据库时间、监控采样和审计都依赖一致时间，多节点时间漂移会破坏事件顺序。

**12. DNS 能解析就代表服务正常吗？**  不能；只证明域名到地址的解析，之后还有路由、TCP、TLS、HTTP、代理、应用和依赖。

### 62.3 Docker

**13. 镜像和容器的区别？**  镜像是只读模板，容器是镜像加运行时配置和可写层形成的实例。

**14. 为什么容器 Running 但服务打不开？**  主进程存活不代表端口已监听、健康检查通过、代理正确或业务依赖可用，应分层检查。

**15. 容器里的 localhost 指什么？**  指当前容器自身，不是宿主机，也不是另一个服务容器。

**16. `3307:3306` 怎么解释？**  宿主机 3307 转发到容器 3306；宿主机程序用 3307，同一 Compose 网络里的应用通常用服务名和 3306。

**17. 为什么使用多阶段构建？**  将编译工具留在构建阶段，运行镜像只包含产物和运行时，减少体积和攻击面。

**18. volume 为什么不是备份？**  它只让数据脱离容器生命周期，仍与主机故障、误删和逻辑损坏处在相近故障域。

**19. `depends_on` 是否保证应用可用？**  只凭启动顺序不够；需要健康条件和应用自身重试/门禁，并最终做业务验收。

**20. 如何减少容器权限？**  非 root 用户、只读根文件系统、最小 capabilities、受限挂载、资源限制和最小镜像，但要通过实际运行验证。

### 62.4 Java/Spring Boot

**21. Controller、Service、Repository 分别做什么？**  Controller 负责 HTTP 边界和校验；Service 编排业务与事务；Repository 负责持久化访问。分层是职责边界，不是为了增加文件。

**22. 为什么 DTO 不直接用 Entity？**  避免数据库模型泄露到接口、限制可写字段、便于校验和版本演进。

**23. 全局异常处理的意义？**  将不同异常映射成稳定的状态码和错误结构，避免泄露堆栈，让前端和监控能一致识别。

**24. 关联 ID 有什么用？**  为一次请求提供贯穿响应和日志的标识，支持从前端错误定位后端事件；它不是完整分布式追踪的替代品。

**25. `@Transactional` 放在哪里？**  放在完整业务用例的 Service 边界，使账户、订单和流水同成同败；还要注意代理调用和异常回滚规则。

**26. 为什么注入 Clock？**  让时间来源可替换，测试可以固定时间，减少不可重复结果。

**27. 如何解释 400、404、409、500？**  400 请求格式/参数不合法，404 资源不存在，409 业务状态或幂等冲突，500 是未处理的服务端错误；具体映射应保持一致。

### 62.5 MySQL、事务与并发

**28. ACID 在这个项目中怎么体现？**  原子性保证转账相关写入同成同败；一致性由约束和业务不变量共同维护；隔离性与锁处理并发；持久性由提交后的数据库日志和存储保证。

**29. 幂等键解决什么？**  客户端因超时重试时，服务能识别同一业务意图并复用结果，避免重复扣款。它不能代替事务。

**30. 为什么幂等快速查询后还要锁后复查？**  两个请求可能同时在第一次查询时都看不到记录；获取锁后再次检查可以关闭竞争窗口。

**31. 为什么按账户 ID 排序加锁？**  让并发事务以一致顺序获取资源，降低 A→B 与 B→A 互相等待导致死锁的概率，但不保证所有死锁消失。

**32. 悲观锁与乐观锁区别？**  悲观锁在事务内先锁住行再操作，适合冲突概率较高的关键更新；乐观锁用版本号在提交时检测冲突，需要重试策略。

**33. 数据库唯一约束是否还需要？**  需要。应用层检查存在并发窗口，唯一约束是数据层最后防线；冲突异常仍要正确映射。

**34. 如何证明余额不足会回滚？**  在请求前后查询两个余额、订单和流水，并用真实 MySQL 集成测试断言没有部分写入。

**35. 为什么测试不用 H2？**  H2 与 MySQL 在方言、锁、事务和索引行为上可能不同；Testcontainers 使用真实 MySQL，更接近目标环境，但启动较慢且依赖 Docker。

**36. 当前是否验证了高并发？**  没有充分验证。已有测试覆盖幂等重放和回滚，但仍应增加多线程并发与压力测试，简历不能提前宣称高并发能力。

**37. Flyway 为什么不能改已执行 V1？**  Flyway 会记录版本和校验，修改历史脚本导致环境不一致；结构变更应新增 V2 及之后版本。

**38. 索引越多越好吗？**  不是。索引提升特定查询，但占空间并增加写入维护成本，应根据查询和执行计划设计。

### 62.6 监控与日志

**39. Prometheus 是接口文档吗？**  不是。它定期拉取数值型时间序列，用于趋势、聚合和告警；接口文档通常由 OpenAPI/Swagger 提供，当前项目未完整接入。

**40. Grafana 存储指标吗？**  通常不负责存储，它查询 Prometheus、Loki 等数据源并可视化。

**41. Loki 与 Prometheus 的区别？**  Prometheus 面向带标签的数值时间序列，Loki 面向日志内容和标签；一个回答多少/是否异常，一个提供事件细节。

**42. Alertmanager 做什么？**  接收 Prometheus 告警，完成分组、去重、路由、抑制、静默和通知；它不采集应用指标。

**43. Pending、Firing、Resolved 是什么？**  条件刚满足并等待持续时间是 Pending，持续满足后 Firing，条件恢复为 Resolved。具体状态还取决于规则和通知配置。

**44. Target up 是否代表业务正常？**  不代表，只说明 Prometheus 能抓到指标端点。业务可能仍返回错误或数据不一致。

**45. 为什么标签不能无限加？**  用户 ID、订单 ID 这类高基数标签会让时间序列爆炸，增加内存和存储压力；细粒度对象应放日志或追踪。

**46. 仪表盘“无数据”怎么查？**  时间范围、数据源、变量、PromQL、指标是否产生、Target 是否 up、标签是否匹配，按链路逐项检查。

**47. 当前告警能发到手机吗？**  默认本地接收器为空时不能；可以在 UI 查看状态，但外部通知需要配置邮件、Webhook 等接收器并保护密钥。

### 62.7 Kubernetes

**48. Pod 与容器是什么关系？**  Pod 是 Kubernetes 最小调度单元，可包含一个或多个共享网络和卷的容器；常见是一 Pod 一主容器。

**49. Deployment 做什么？**  声明无状态应用副本和更新策略，通过 ReplicaSet 维持期望副本并执行滚动更新。

**50. Service 为什么需要？**  Pod 地址会变化，Service 通过选择器提供稳定虚拟地址和负载分发。

**51. Ingress 与 Service 区别？**  Service 负责集群内稳定访问和四层/基础暴露；Ingress 基于 HTTP 主机和路径做七层入口，需要 Ingress Controller。

**52. liveness 与 readiness 区别？**  liveness 失败可能触发容器重启；readiness 失败会停止接收 Service 流量。启动慢还可使用 startupProbe。

**53. requests 与 limits 有什么用？**  requests 用于调度和资源保障参考，limits 限制最大使用；内存超限可能 OOMKilled，CPU 超限通常被节流。

**54. HPA 为什么显示 unknown？**  可能 metrics-server 不可用、Pod 缺少 requests、指标尚未产生或 API 不通。先查 HPA 条件、metrics API 和 Pod 资源配置。

**55. PDB 能保证高可用吗？**  不能。它主要限制自愿中断时同时不可用的副本数量，不阻止节点突然故障，也不能弥补单节点集群。

**56. StatefulSet 为什么用于 MySQL？**  提供稳定身份和持久卷绑定，适合有状态工作负载；单副本 StatefulSet 仍不是数据库高可用。

**57. Secret 是否加密？**  YAML 中常见 Base64 只是编码。是否静态加密取决于集群配置，还需 RBAC 和外部密钥管理。

**58. NetworkPolicy 写了就生效吗？**  不一定；CNI 必须支持策略，并要实测允许和拒绝路径。

**59. ImagePullBackOff 怎么查？**  查看 Pod describe 和 Events，核对镜像名、标签、仓库权限、网络和架构；Minikube 网络受限时可加载已构建本地镜像。

**60. 单机 Minikube 能证明什么？**  能验证 YAML/Helm、服务发现、探针、控制器和基本流程；不能证明多节点容灾、真实负载、生产网络或存储高可用。

### 62.8 发布、安全与运维综合

**61. 为什么发布后还要冒烟测试？**  进程和健康端点通过只证明基础可用，冒烟测试验证关键用户路径和代理链路。

**62. 回滚为什么不一定能恢复？**  数据库迁移或业务数据可能已经发生不可逆变化，旧代码未必兼容新结构。因此要做向后兼容迁移和发布前检查。

**63. RPO 与 RTO 如何解释？**  RPO 是可接受的数据丢失窗口，RTO 是可接受的恢复时间；必须由业务要求驱动并通过演练验证。

**64. 为什么项目不能直接上公网？**  当前缺少完整认证授权、TLS、WAF、密钥治理、限流和生产审计等控制，仅适合受控学习环境。

**65. AI 参与后项目还算自己的吗？**  取决于是否能审查、运行、测试、修改和解释。应如实说明 AI 辅助范围，并用独立实操和证据建立能力所有权。

**66. 你遇到的最典型故障是什么？**  应选择真实经历，按现象—假设—证据—根因—修复—验证—预防回答，不能编造。可以使用镜像拉取失败、监控入口不可用等已实际验证案例。

**67. 下一步最值得做什么？**  先补并发测试、CI/CD、认证授权和外部告警之一，并形成测试或演练证据；不继续无目的扩张组件。

<a id="chapter-63"></a>

## 第 63 章　简历表述、AI 参与和证据等级

### 63.1 简历只写能被追问的内容

推荐使用“动作 + 技术 + 问题 + 结果/证据”的句式。例如：

> 基于 Spring Boot 与 MySQL 实现账户转账，使用事务、幂等键和账户有序悲观锁维护扣款、入账、订单与双流水的一致性，并以真实 MySQL 集成测试验证重复请求和异常回滚。

如果没有并发压测数据，就不要写“支持高并发”；如果只在 Minikube 运行，就不要写“搭建生产级高可用集群”；如果 Alertmanager 只有 UI，就不要写“实现企业微信告警”。

### 63.2 证据分级

| 等级 | 含义 | 示例 |
|---|---|---|
| E0 | 只有配置或代码 | 仓库存在 HPA YAML |
| E1 | 能在本机运行 | HPA 对象创建成功 |
| E2 | 有自动化验收 | 测试、脚本、状态输出 |
| E3 | 有故障与恢复证据 | 故障注入、告警、恢复时间 |
| E4 | 有持续运行数据 | 长期 SLO、容量和事件记录 |

当前简历尽量使用 E2—E3 证据。E0 只能说明“设计或配置”，不能写成“完成稳定运行”。

### 63.3 AI 参与的合适说法

可以写在项目说明或面试回答中：

> 在项目开发中使用 Codex 辅助进行架构拆分、代码审查、测试用例设计和运维文档沉淀；本人负责需求取舍、环境运行、故障复现、关键代码理解与验收，并对 AI 生成内容进行修改和验证。

不建议把 Codex 与 Linux、Docker 并列成一项“基础设施技术栈”。AI 是工作方法，应落在具体活动和质量控制上。

### 63.4 最终能力所有权清单

在投递前，你应该能独立完成：

- 不依赖 AI 启动完整 Compose 并关闭；
- 解释每个容器和端口；
- 从前端操作追到 Java 方法和 MySQL 记录；
- 手写一条账户/流水查询 SQL；
- 解释并验证事务、幂等和锁顺序；
- 完成一次 Linux 服务定位；
- 完成一次 Prometheus 查询和 Loki 日志检索；
- 完成一次 Pod 故障定位；
- 修改一个小功能并补测试；
- 做一次发布、回滚和故障复盘；
- 明确说出至少五个尚未生产化的边界。

若其中某项暂时不能完成，不需要否定整个项目。把它标记为学习任务，降低简历措辞等级，完成后再升级表述。

<a id="part-01"></a>

# 第十篇：故障排查百科与值班参考

<a id="chapter-64"></a>

## 第 64 章　证据驱动的排障方法

排障的目标不是“让报错消失”，而是用证据找到导致用户影响的最小根因，安全恢复，并防止复发。尤其在跨越浏览器、代理、Java、MySQL、容器和 Kubernetes 的系统中，盲目重启会清除现场、改变时间线，还可能让偶发问题暂时消失。

### 64.1 先明确现象

把“系统坏了”改写成可验证描述：

- 谁在什么时间、从什么入口访问；
- 哪个操作失败；
- 得到的状态码或错误文本；
- 全部用户还是单个请求；
- 持续失败还是偶发；
- 最近是否发布、改配置、改网络或清理数据。

好的现象描述示例：`14:20 起本机访问 http://localhost:18000 首页正常，但提交转账返回 500；账户查询仍为 200；同一时间 app 日志出现 MySQL lock wait timeout。`

### 64.2 建立分层假设

```text
用户/浏览器
  -> 名称解析
  -> TCP 连接
  -> HTTP/Nginx
  -> Spring Boot 路由与业务
  -> 数据库/外部依赖
  -> 数据正确性
```

先用一个最小测试定位失败边界。例如浏览器页面打不开，但直接请求 `localhost:18080/actuator/health` 成功，那么 Java 基础存活，优先检查 Web/Nginx 和端口，而不是先修改数据库。

### 64.3 四类证据

1. **状态**：进程、容器、Pod、Service、健康端点；
2. **日志**：错误文本、堆栈、关联 ID、数据库日志、Event；
3. **指标**：错误率、延迟、资源、连接池、Target；
4. **数据**：表记录、余额、订单、流水和迁移版本。

状态告诉你“现在怎样”，日志说明“发生过什么”，指标显示“何时开始和影响多大”，数据验证“业务是否真的正确”。四者互相补充。

### 64.4 时间线

统一时区，记录：首次告警、用户影响、最近变更、每个排查动作、恢复时间和验证时间。一次只改一个因素。若 14:31 同时重启应用、改密码、重载 Nginx，之后恢复，你无法证明哪个动作有效。

### 64.5 排障终点

同时满足以下条件才算结束：用户路径恢复；关键数据正确；错误率/延迟回到基线；持续观察一个合适窗口；临时改动已记录；根因与改进项进入复盘。

<a id="chapter-65"></a>

## 第 65 章　启动与入口故障

### 65.1 Docker 命令无法连接引擎

**现象**：客户端提示无法连接 daemon、pipe 不存在或只有 Client 信息。

**判断链**：Docker Desktop 是否 Running → 当前是否 Linux 容器模式 → Docker context 是否正确 → WSL2/虚拟化后端状态 → 磁盘/权限。

**证据**：`docker version`、`docker context ls`、Docker Desktop 状态页和诊断信息。

**处理**：先恢复引擎，不要重建项目。引擎恢复后重新检查现存容器和卷。重启 Docker Desktop 是平台恢复动作，不应同时删除 Compose 卷。

**复述**：命令行工具存在不等于服务端引擎可用，客户端和 daemon 是两个部分。

### 65.2 端口被占用

**现象**：绑定端口失败，或访问入口却出现另一个应用页面。

**判断链**：确认报错端口 → 找监听 PID/容器 → 核对进程所有者和用途 → 决定停止冲突服务或改映射。

**证据**：Windows 使用监听连接与进程信息，Linux 使用 `ss -lntp`，Docker 使用容器端口列表。

**处理**：不要直接结束未知 PID。若更改端口，同步修改前端代理、文档、健康脚本和监控目标。OpsPilot 使用 18000/18080 等非默认端口，就是为了降低 8080 常见占用风险，但仍不能保证永不冲突。

### 65.3 前端首页连接被拒绝

**现象**：浏览器立即显示无法连接，没有 HTTP 状态码。

**判断链**：URL 与端口 → 宿主机是否监听 18000 → web 容器状态 → 端口映射 → 本机代理/防火墙。

**处理**：若 web 未启动，查看容器退出日志；若已监听，使用本机 HTTP 请求排除浏览器缓存或代理；若容器正常但没有映射，核对 Compose 展开配置。

### 65.4 首页可开但刷新子路由 404

**根因方向**：React SPA 的前端路由由浏览器处理，Nginx 收到 `/accounts` 时若按静态文件查找会找不到。

**处理**：配置 `try_files` 回退 `index.html`，但 `/api` 等后端路径不能被错误回退。检查 Nginx 语法并平滑重载。

**验收**：直接在地址栏访问每个路由并刷新，静态资源和 API 都正常。

### 65.5 页面能开但全部业务卡片无数据

**判断链**：浏览器 Network 是否发请求 → 请求 URL → 状态码与响应体 → Nginx 代理日志 → app 日志 → 数据库记录。

常见原因：前端使用错误 API 基址；Nginx `/api` 路径重写错误；后端未启动；跨域；数据库为空；前端把错误吞掉并显示空状态。

**验收**：不能只让页面“不报错”，需要查询数据库或 API 确认真实数据，并检查前端错误状态能够告诉用户失败。

### 65.6 PowerShell 启动脚本执行策略阻止

**现象**：脚本无法加载或被策略阻止。

**处理思路**：确认脚本来源和内容，使用组织允许的签名或进程级策略运行，不要为了一个项目永久放开全机安全策略。记录实际执行命令和作用范围。

### 65.7 构建依赖下载失败

**判断链**：DNS → HTTPS 连接 → 代理 → Maven/npm 仓库 → 凭据 → 版本是否存在 → 本地缓存是否损坏。

**处理**：保留完整错误，区分连接超时和依赖不存在。企业网络可能需要受控镜像仓库。不要反复删除所有缓存；先定位具体依赖和仓库。

### 65.8 容器启动顺序正确但应用仍连接不上 MySQL

**原因**：容器启动不等于数据库已经接受连接；健康检查可能配置不准确；应用没有重试；地址使用了 `localhost`。

**证据**：MySQL 健康状态、3306 监听、app 使用的 JDBC URL、应用首次连接时间、数据库日志。

**处理**：使用健康条件、应用连接重试和健康门禁；容器间用服务名 `mysql:3306`。

<a id="chapter-66"></a>

## 第 66 章　网络、Nginx 与 HTTP 故障

### 66.1 域名解析失败

**现象**：找不到主机名；用 IP 访问可能正常。

**判断链**：拼写/搜索域 → 本机 DNS 配置 → 查询结果 → hosts 文件 → 企业 DNS/公共 DNS → 缓存。

**证据**：解析命令输出、DNS 服务器地址和返回码。不要用 ping 成败作为唯一证据，因为目标可能禁 ICMP。

**处理**：修正记录或解析配置，清理缓存仅在确认缓存问题时进行。临时 hosts 映射必须记录并及时移除。

### 66.2 TCP 连接超时

**含义**：客户端在等待连接建立，常见于路由、ACL、防火墙、安全组、NAT、目标不可达或严重丢包。

**证据链**：本机路由 → 到目标路径 → 防火墙策略 → 目标监听 → 抓包中是否发出 SYN、是否收到 SYN-ACK/RESET。

若只能看到重复 SYN 没有响应，不能直接断定“服务器没启动”；也可能响应路径或中间策略丢弃。

### 66.3 TCP 连接拒绝

目标地址可达但端口没有监听，或设备主动返回拒绝。查目标 `ss`、服务状态、监听地址是 `127.0.0.1` 还是 `0.0.0.0`、容器映射和防火墙 reject 规则。

### 66.4 Nginx 502 Bad Gateway

**常见根因**：upstream 主机/端口错误；后端未监听；容器 DNS 解析失败；协议错（HTTP/HTTPS）；权限或网络策略拒绝；后端在建立连接后异常关闭。

**排查顺序**：读取 Nginx error log 的具体错误 → 在 Nginx 所在网络命名空间直接请求 upstream → 查后端监听与日志 → 查配置展开。

**注意**：从宿主机能访问 `localhost:18080`，不代表 Nginx 容器能访问这个地址；容器应使用 `app:8080`。

### 66.5 Nginx 504 Gateway Timeout

**常见根因**：慢 SQL、锁等待、线程池/连接池耗尽、后端阻塞、代理超时过短。

**排查**：先看请求实际耗时和后端是否仍执行，再查数据库锁、慢查询、JVM 资源和连接池。随意把 timeout 从 60 秒改成 10 分钟只会让用户等更久，不能消除根因。

### 66.6 HTTP 404

区分由谁返回：浏览器静态路由、Nginx、Spring Boot 还是业务资源不存在。查看响应头、响应体格式和相应日志。若 API 路径前缀被 Nginx 重写错误，后端可能收到不存在的路径。

### 66.7 HTTP 400

通常是 JSON 格式、字段类型、必填项、金额范围或校验失败。检查浏览器实际请求体与后端 DTO，不要仅看前端表单显示值。统一错误响应应指出可修正字段，不泄露内部堆栈。

### 66.8 HTTP 409

OpsPilot 可用于业务冲突，如余额不足、幂等键复用但参数不一致、版本冲突。409 说明请求语法可能正确，但与当前资源/业务状态冲突。查看错误代码和关联 ID，验证数据库没有部分更新。

### 66.9 HTTP 500

表示服务端未正确处理异常。用关联 ID 查后端堆栈，确定是代码、数据库、配置还是依赖。修复后补测试或异常映射，不能只让前端把 500 文案隐藏。

### 66.10 CORS 错误

开发时浏览器从 Vite 端口调用后端可能跨源。优先使用开发代理保持同源，或明确配置允许的来源、方法和头。生产不应无条件允许所有来源并携带凭据。

### 66.11 TLS/证书错误

检查系统时间、证书域名、有效期、证书链和协议。跳过证书验证只适合作为短暂定位，不是修复。OpsPilot 本地默认 HTTP，生产演进必须在 Nginx/Ingress 配置 TLS 并管理续期。

### 66.12 偶发网络慢

同时观察 DNS 时延、TCP 重传、应用延迟分位数、JVM 暂停、数据库慢查询和资源饱和。单次 ping 平均值不能解释应用尾延迟。抓包前限定接口、主机和端口，避免收集无关敏感流量。

<a id="chapter-67"></a>

## 第 67 章　Spring Boot、MySQL 与业务一致性故障

### 67.1 应用启动后立即退出

**判断链**：退出码 → 最后一段日志 → 配置解析 → 端口 → 数据库 → Flyway → Bean 创建。

常见错误文本包括端口占用、JDBC 连接失败、迁移校验失败、环境变量缺失、Bean 循环依赖。日志要从最早的 `Caused by` 链和根异常判断，不要只复制顶部通用错误。

### 67.2 数据库 Access denied

核对用户名、连接来源、密码、授权范围和实际连接实例。MySQL 账户常与 host 组合匹配；`user@localhost` 与远程来源可能不是同一授权。避免把管理员账号直接给应用。

### 67.3 Communications link failure

表示 JDBC 连接链路未建立或中断。检查主机名解析、端口、MySQL 监听、容器网络、TLS 参数和空闲连接失效。结合 MySQL 日志区分服务器拒绝、网络中断和客户端配置。

### 67.4 Unknown database 或表不存在

确认连接到哪个实例和 schema，Flyway 是否运行、迁移账户权限、`flyway_schema_history` 状态。不要在错误环境手工建表掩盖迁移问题。

### 67.5 Flyway checksum mismatch

通常是已经执行的历史迁移被修改。对已使用环境，恢复原 V1 内容并用新版本脚本进行变更。`repair` 会改变历史记录，必须理解原因并有审批，不能作为常规“消错”按钮。

### 67.6 连接池耗尽

**现象**：请求等待连接超时，数据库可能仍在线。

**检查**：活动/空闲/等待连接指标；线程堆栈；慢 SQL；事务持续时间；连接泄漏；池大小与数据库上限。

**处理**：优先缩短事务、修复慢 SQL/泄漏，再基于容量调池。盲目加大池可能把压力转移到 MySQL。

### 67.7 慢 SQL

记录具体 SQL、参数范围、耗时和执行计划。检查是否全表扫描、索引选择、排序/临时表、返回行数、锁等待。优化后在接近实际数据量上复测，不能只在三行数据上证明。

### 67.8 死锁

数据库选择一个事务回滚以打破循环。保存死锁报告、涉及 SQL、锁顺序和事务时间。OpsPilot 通过账户 ID 排序减少典型双账户反向转账死锁，但其他表或索引仍可能形成不同顺序。应用应把可重试异常与永久业务失败区分，并限制重试次数和退避。

### 67.9 Lock wait timeout

某事务等待锁超过阈值。查阻塞者、被阻塞者、长事务和未提交会话。不要立刻调大超时；先处理为什么锁持有太久。结束数据库会话属于破坏性操作，必须确认事务影响。

### 67.10 重复转账

收集幂等键、请求体、订单记录、流水和调用日志。若同一业务意图使用不同幂等键，服务端单靠键无法识别；调用方必须为一次业务意图稳定复用键。若同键产生重复数据，检查唯一约束、事务边界和竞争窗口。

### 67.11 同一幂等键返回冲突

表示调用方复用了键但改变了源账户、目标账户或金额。这是正确拒绝行为，不应简单删除幂等记录再重试。调用方应为新业务生成新键。

### 67.12 余额出现负数

立即停止相关写入并保留证据。检查是否存在绕过 Service 的更新、并发检查更新分离、数据库约束缺失、旧版本代码或人工 SQL。恢复必须基于审计和业务确认，不能直接把负数改成零。

### 67.13 成功订单没有两条流水

查询订单、账户、流水、应用版本和事务日志。如果写入处于同一事务，理论上不应部分提交；出现时可能是历史数据、手工修改、事务边界未生效或查询条件错误。先证明数据事实，再判断代码。

账务核验 SQL 应检查每个成功订单的流水数量、借贷方向和金额，并汇总异常订单。修复前备份并记录变更脚本。

### 67.14 前端显示成功但数据库没数据

确认响应来自真实后端而非 mock；确认查询的是同一个数据库实例/schema；检查事务是否最终回滚；核对前端是否仅根据按钮提交状态显示成功。成功 UI 必须依赖后端确认结果。

### 67.15 readiness 为 UP 但数据库已停止

这可能是健康组没有包含数据库指标。检查 Actuator 实际响应和 Spring Boot 健康组配置，不能依据注释推断。决定是否纳入数据库时要权衡：数据库短暂抖动会让所有 Pod 同时 not ready，可能导致服务完全无端点；也可通过降级和更精细的可用性语义处理。

### 67.16 JVM 内存持续增长

观察堆使用、GC 后基线、对象分配、线程和容器内存限制。缓存增长不一定是泄漏；真正泄漏通常在多次 GC 后基线仍增长。先获取受控诊断证据，再做堆转储；堆转储可能包含敏感数据且文件很大。

### 67.17 CPU 高

先确认是宿主机、容器还是某个线程；结合请求量、GC、热点线程和慢查询。高 CPU 可能是正常高负载，也可能是死循环、序列化、日志风暴或频繁 GC。限流和扩容用于止损，仍需找代码或容量根因。

### 67.18 日志出现敏感信息

立即限制访问并评估泄露范围，停止继续打印，轮换已暴露凭据，清理和保留应遵守审计规则。修复日志脱敏和异常处理，增加自动检查。简单删除文件可能破坏证据，不能擅自操作。

<a id="chapter-68"></a>

## 第 68 章　Docker 与 Compose 故障

### 68.1 容器 Exited (0)

退出码 0 表示主进程认为正常结束。对于常驻服务，这仍是异常现象：可能启动脚本执行完但没有以前台方式运行服务。检查镜像的 ENTRYPOINT/CMD 和 PID 1。

### 68.2 容器 Exited (1)

通用应用错误。先看容器日志和 inspect 中的启动参数、环境、挂载，不要仅凭退出码判断具体原因。

### 68.3 Exited (137) 或 OOMKilled

137 常与 SIGKILL 有关，可能是内存超限，也可能是人为强制停止。Docker/Kubernetes 状态、宿主机内核日志和内存指标共同确认。若是 OOM，检查 JVM 堆与容器上限，不能只增加重启次数。

### 68.4 容器 unhealthy

查看 healthcheck 的命令、间隔、超时、重试和最近输出。健康命令在容器内执行，所需工具必须存在；访问地址通常是容器内部端口。健康检查失败可能是检查本身写错。

### 68.5 容器间无法解析服务名

确认两个容器是否在同一 Compose 网络、服务名拼写、网络别名和容器是否运行。宿主机 DNS 与 Docker 内置 DNS 是不同路径。

### 68.6 挂载后配置消失或应用无法写入

绑定挂载会覆盖镜像原目录内容。检查源路径是否正确、Windows 路径共享、文件/目录类型、读写权限和只读标记。非 root 容器常因宿主机目录所有权失败。

### 68.7 镜像构建缓存导致“修改未生效”

先确认修改文件是否进入 build context、`.dockerignore` 是否排除、COPY 顺序和镜像标签。查看构建输出哪些层使用缓存。不要默认使用完全无缓存构建，先找缓存键设计问题。

### 68.8 镜像拉取失败

区分仓库不存在、标签不存在、未登录、限流、DNS/代理和平台架构不匹配。保留具体 HTTP/daemon 错误。Minikube 可加载本地镜像，但要设置合适的拉取策略并确认 Pod 使用该镜像。

### 68.9 磁盘空间被镜像/日志占满

用 Docker 磁盘统计区分镜像、构建缓存、容器和卷。清理前列出精确对象，确认未使用和数据可恢复。生产应配置日志轮转、镜像保留和容量告警，而不是定期无差别 prune。

### 68.10 `docker compose down -v` 后数据消失

`-v` 删除项目卷，属于预期行为而非数据库自动损坏。若没有备份只能从其他副本/导出恢复。预防方式是危险操作前确认环境、卷名和备份，脚本默认不带卷删除。

### 68.11 修改环境变量后容器仍是旧配置

仅 restart 通常不会重新创建容器并应用新的 Compose 环境。检查容器 inspect 中实际环境，使用受控重建，再做健康和业务验收。密钥不要直接出现在共享输出中。

### 68.12 Compose 项目名变化导致出现两套容器

项目名可能受目录名或 `-p` 影响。查看 Compose project 标签和网络/卷名称，确认浏览器连接哪一套。统一项目名和工作目录，清理旧环境前核对其数据。

<a id="chapter-69"></a>

## 第 69 章　Prometheus、Grafana、Loki 与告警故障

### 69.1 Prometheus 页面打不开

检查 19090 是否监听、容器状态、端口映射、日志和 Docker 引擎。若 Minikube 与完整 Compose 不能同时运行是本机资源/端口策略问题，应明确当前选择的环境，不要把两套入口混用。

### 69.2 Prometheus Target 为 DOWN

打开 Targets 查看最后错误：DNS、连接拒绝、超时、404、认证或解析格式。然后从 Prometheus 容器网络直接访问目标。宿主机能访问并不能证明 Prometheus 容器能访问。

### 69.3 Target 为 UP 但查询无数据

检查指标名、时间范围、标签、抓取时间和应用是否产生该指标。某些 HTTP 指标只有接口被访问后才出现。用指标浏览器逐步缩小，不要直接在复杂面板表达式上猜。

### 69.4 PromQL 返回很多重复线

检查 `by(...)` 保留的标签和实例副本。聚合前明确问题：看全局请求率、按状态码、按 URI 还是按实例。错误聚合会把不同语义混在一起。

### 69.5 请求率异常高

可能是真实流量、健康检查、监控抓取、前端轮询、重试风暴或标签口径。将指标按 URI/status/instance 分组，结合访问日志验证。先限流/停止异常调用方，再修复重试策略。

### 69.6 错误率告警误报

低流量下 1 个失败可能导致 100% 错误率。规则应同时考虑请求量门槛、时间窗口、排除健康端点和状态码定义。修改规则后使用历史数据或测试流量验证。

### 69.7 Grafana 登录页可开但仪表盘无数据

检查数据源连接测试、URL（Grafana 容器内应使用服务名）、时间范围、变量、面板查询和 Prometheus 原始数据。容器里的 `localhost:19090` 通常不是 Prometheus 服务。

### 69.8 Grafana 仪表盘丢失

确认是否使用了正确数据卷、provisioning 文件是否挂载、容器是否被带卷删除、登录的是不是另一套 Compose 项目。重要仪表盘应以 JSON/配置代码化并纳入版本管理。

### 69.9 Loki 无日志

链路是容器日志 → Alloy 发现/读取 → Loki 写入 → Grafana 查询。逐段检查 Alloy 日志与目标、挂载的 Docker 日志路径、Loki ready、标签和时间范围。不要只重启 Grafana，因为它只是查询端。

### 69.10 Loki 查询不到关联 ID

先确认应用日志真的输出该 ID、请求命中了目标实例、日志时间与查询时区、Alloy 是否采集该容器。关联 ID 应作为日志内容或低基数上下文使用；不要把每个唯一 ID 作为 Loki 索引标签造成高基数。

### 69.11 Alertmanager 页面能开但没有告警

可能 Prometheus 规则未加载、条件未满足、仍 Pending、Prometheus 未配置 Alertmanager 或告警被静默。先在 Prometheus Rules/Alerts 看规则状态，再查 Alertmanager。

### 69.12 Alertmanager 有告警但未通知

检查路由匹配、receiver、发送集成、凭据、网络和通知日志。OpsPilot 当前本地接收器用于 UI 观察，没有配置外部通道时，不发送邮件/企业微信是预期状态。

### 69.13 告警一直 Firing 但业务已恢复

检查规则表达式和时间窗口、指标是否继续上报旧值、`for`/恢复条件、目标是否仍 down、Prometheus 时间。避免仅在 Alertmanager 静默；静默只隐藏通知，不改变根因或规则状态。

### 69.14 告警风暴

先按服务/集群分组和抑制下游症状，例如数据库故障可能引起几十个 API 告警。保留一个能指向根因的主告警，减少重复通知。之后调整规则依赖和路由，不要简单关闭所有告警。

<a id="chapter-70"></a>

## 第 70 章　Kubernetes 故障

### 70.1 `kubectl` 无法连接集群

检查当前 context、Minikube 状态、API Server 地址和 kubeconfig。不要对未知 context 执行变更。先输出当前上下文和目标 namespace。

### 70.2 Pod Pending

查看 describe 的 Events。常见原因：资源不足、PVC 未绑定、节点选择/污点不匹配、镜像拉取前置问题。Pending 不是容器日志问题，因为容器可能尚未启动。

### 70.3 ImagePullBackOff

检查完整镜像名和标签、仓库认证、网络、架构和拉取策略。等待会指数退避，但不会自动修正错误标签。Minikube 本地镜像加载后要确认镜像名称完全一致。

### 70.4 CrashLoopBackOff

容器反复启动失败并退避。查看当前日志和 `--previous` 上一次日志、退出码、环境、挂载、探针。若应用在探针前就退出，先处理应用错误；若只有探针杀死，调试探针路径、端口和启动时间。

### 70.5 Pod Running 但不 Ready

查看 readiness 失败详情、Pod 条件和 endpoints。Running 只表示容器进程存在。验证端点从 Pod 内、同 namespace 和 Service 三个位置的差异。

### 70.6 Service 没有 Endpoints

常见是 selector 与 Pod labels 不匹配，或 Pod 未 Ready。对比 Service selector 与 Pod 标签，查看 EndpointSlice。不要先删除 Service。

### 70.7 Service 有 Endpoints 但访问失败

检查 targetPort、容器监听地址、NetworkPolicy、应用协议和 kube-proxy/CNI。应用若只监听 `127.0.0.1`，其他 Pod 不能访问。

### 70.8 Ingress 404/502

先确认 Ingress Controller 存在并就绪，再看 Ingress class、host/path、Service、Endpoints。404 常由路由未匹配，502 常由后端连接失败。Minikube 环境若 Controller 镜像或 webhook 不可用，可使用端口转发作为实验入口，并诚实记录限制。

### 70.9 PVC Pending

检查 StorageClass、默认类、动态 provisioner、访问模式和容量。单机 Minikube 的默认存储行为与生产云盘/NFS 不同，数据可靠性不能等同。

### 70.10 Pod OOMKilled

查看 lastState、limits、工作集指标和 JVM 参数。JVM 不只使用堆，还有 metaspace、线程栈、直接内存。容器 limit 不能简单等于 `-Xmx`，需要留出非堆空间。

### 70.11 CPU throttling

CPU limit 过紧时，即使节点还有 CPU，容器也可能周期性被节流，引起尾延迟。结合 CPU 使用、throttled 指标和请求延迟判断；优化代码/容量后再调整 limit。

### 70.12 HPA target unknown

检查 metrics-server Pod、APIService、`kubectl top`、工作负载的 CPU requests 和 HPA conditions。没有 requests 时，CPU 使用百分比缺少分母。Minikube 拉不到 metrics-server 镜像时，记录限制，不宣称弹性验证成功。

### 70.13 HPA 不扩容

确认指标超过目标并持续满足、当前是否达到 maxReplicas、扩缩策略和稳定窗口。还要产生足够且可重复的负载。只创建 HPA 对象不是扩容证据。

### 70.14 Deployment 滚动更新卡住

查看 rollout status、ReplicaSet、不可用 Pod 的探针/镜像/资源、maxUnavailable/maxSurge 和 PDB。若新版本永远不 Ready，滚动发布应保留部分旧副本；根据影响决定修复还是回滚。

### 70.15 PDB 阻止节点维护

PDB 保护最小可用副本，副本数过少或 minAvailable 过高时，驱逐会被阻止。评估业务可用性、临时扩容或调整策略需有审批，不能为排空节点直接删除保护。

### 70.16 NetworkPolicy 导致全断

默认拒绝后，需要显式允许 DNS、前端到后端、后端到 MySQL、Prometheus 抓取等必要流量。使用测试 Pod 验证允许和拒绝路径，确认 CNI 支持。策略回滚前保存配置和影响。

### 70.17 ConfigMap 修改未生效

环境变量方式注入的配置通常需要重建 Pod；卷挂载文件可能最终更新，但应用未必热加载。检查 Pod 内实际值和 Deployment 模板哈希，执行受控 rollout。

### 70.18 Secret 更新后旧 Pod 仍用旧值

与注入方式有关。通过环境变量注入时需要重建 Pod；轮换数据库密码应设计新旧凭据过渡，避免所有实例同时断连。不要在 describe/命令历史中暴露明文。

### 70.19 Helm upgrade 失败

查看渲染后的 manifest、values 差异、release 状态和 hook。先在客户端 lint/template，再对测试 namespace dry-run。回滚后验证资源与业务，因为数据库变更可能不可逆。

### 70.20 删除 Pod 后没有重建

确认 Pod 是否由 Deployment/StatefulSet 管理、控制器副本数、namespace 和控制器状态。裸 Pod 删除后不会自动重建；这正说明控制器而非 Pod 本身提供期望状态。

### 70.21 MySQL Pod 重建后数据丢失

检查 PVC 是否仍绑定、挂载路径、是否误删 PVC/底层 volume、应用连接的是不是新实例。StatefulSet 名称稳定不代表数据必然安全；备份仍是独立要求。

### 70.22 节点 NotReady

查看节点 Conditions、kubelet、容器运行时、磁盘/内存/PID 压力和网络。单机 Minikube 节点 NotReady 会影响全部工作负载，没有第二节点承接，说明它不是 HA 环境。

<a id="chapter-71"></a>

## 第 71 章　命令不是答案：按问题查工具

以下命令用于学习和受控排查。执行前确认环境、权限和目标。涉及删除、驱逐、结束进程、修改防火墙和数据库会话的动作不列为默认步骤。

### 71.1 Linux 系统与服务

| 想知道什么 | 常用入口 | 观察重点 |
|---|---|---|
| 系统版本 | `cat /etc/os-release` | 发行版和版本，不凭界面猜 |
| CPU/架构 | `lscpu`、`uname -m` | 架构影响镜像和软件包 |
| 内存 | `free -h` | available、swap，不只看 free |
| 磁盘 | `df -hT` | 使用率、挂载点、文件系统 |
| 目录占用 | `du -xhd1 <目录>` | 限定文件系统和精确目录 |
| 进程 | `ps -ef`、`top` | PID、用户、CPU/内存、命令 |
| 监听端口 | `ss -lntp` | 地址、端口、PID/程序 |
| 服务状态 | `systemctl status <服务>` | Active、退出码、最近日志 |
| 服务日志 | `journalctl -u <服务>` | 时间、首次错误、重启循环 |
| 内核事件 | `dmesg`/journal | OOM、磁盘、网络驱动 |
| 时间 | `timedatectl` | 时区和同步状态 |

`top` 的瞬时高值需要结合持续时间和请求量；`df` 空间充足但仍不能写，可能是 inode、权限、只读挂载或配额问题。

### 71.2 网络

| 问题 | 工具 | 解释边界 |
|---|---|---|
| 地址与接口 | `ip -br addr` | 是否有预期 IP/接口状态 |
| 路由 | `ip route` | 默认路由和目标网段 |
| DNS | `dig`/`nslookup` | 解析服务器、返回地址/错误码 |
| TCP 端口 | `nc`/`telnet`/TCPing | 连接建立，不证明 HTTP 正常 |
| HTTP | `curl -v` | DNS、连接、请求、状态码、头 |
| 路径 | `traceroute`/`tracert` | 中间设备可能不响应，结果需谨慎 |
| 抓包 | `tcpdump`/Wireshark | 限定接口/主机/端口，注意隐私 |

网络排查记录应包含源地址、目标地址、端口、协议和时间。只说“我 ping 过了”信息不足。

### 71.3 Docker/Compose

| 问题 | 命令 | 观察重点 |
|---|---|---|
| 引擎 | `docker version` | Client 与 Server |
| Compose 配置 | `docker compose config` | 展开后的变量、端口、挂载 |
| 服务状态 | `docker compose ps` | 状态、健康、端口 |
| 日志 | `docker compose logs <服务>` | 启动链和第一根因 |
| 容器详情 | `docker inspect <容器>` | 环境、网络、挂载、健康、退出 |
| 资源 | `docker stats` | CPU、内存、网络、块 I/O |
| 网络 | `docker network inspect <网络>` | 成员和地址 |
| 卷 | `docker volume inspect <卷>` | 挂载位置和标签 |
| 磁盘 | `docker system df` | 镜像、缓存、卷占用 |

进入容器执行命令只是定位手段，容器内的临时修改会在重建后消失。正式修复应回到 Dockerfile、配置或编排文件。

### 71.4 MySQL

| 问题 | 观察内容 |
|---|---|
| 当前实例 | 主机、端口、schema、server version |
| 迁移 | `flyway_schema_history` 版本和成功状态 |
| 连接 | process list、连接数、等待会话 |
| 慢查询 | 慢日志、执行计划、扫描/返回行数 |
| 锁 | 当前事务、锁等待、死锁报告 |
| 数据一致性 | 账户、订单、流水关联与汇总 |
| 容量 | 表大小、索引大小、增长速度 |

直接在数据库修改数据前必须备份和记录 SQL。生产场景还需要审批、双人复核和审计。

### 71.5 Kubernetes

| 问题 | 命令入口 | 观察重点 |
|---|---|---|
| 上下文 | `kubectl config current-context` | 防止操作错集群 |
| 资源状态 | `kubectl get ... -n <ns>` | Ready、Status、重启、年龄 |
| 详细事件 | `kubectl describe ...` | Conditions、Events |
| 日志 | `kubectl logs` | 当前与 previous、容器名 |
| 配置 | `kubectl get ... -o yaml` | 实际对象，不只看本地文件 |
| 服务端点 | `kubectl get endpointslice` | selector 与 Ready 后端 |
| 资源指标 | `kubectl top` | 依赖 metrics-server |
| 发布 | `kubectl rollout status/history` | 进度、版本、失败副本 |
| Helm | `helm list/status/get values` | release 和实际 values |

默认先使用读取类命令。删除 Pod 可以作为明确的测试或恢复动作，但先确认控制器、业务影响和目标 namespace。

<a id="chapter-72"></a>

## 第 72 章　核心术语词典

**ACL**：访问控制列表，根据源、目标、端口等规则允许或拒绝流量。

**Actuator**：Spring Boot 提供的运行端点体系，可暴露健康、指标等管理信息。

**Alert**：某个规则条件在时间窗口内满足形成的异常状态，不等于一定发生用户影响。

**Alertmanager**：负责告警的分组、去重、路由、静默和通知。

**API**：系统之间约定的调用接口，包括路径、方法、参数、响应与错误语义。

**CNI**：Kubernetes 容器网络接口规范及其插件实现，负责 Pod 网络等能力。

**Container**：镜像的运行实例，具有隔离的进程、文件系统视图和网络空间。

**Correlation ID**：关联一次请求的标识，用于跨日志定位。

**CPU limit/request**：Kubernetes 中资源上限与调度需求；二者用途不同。

**CRUD**：创建、读取、更新、删除，是数据操作基础，但不能代表完整工程能力。

**Deployment**：Kubernetes 管理无状态应用副本和滚动更新的控制器。

**DNS**：把域名解析为地址及其他记录的系统。解析成功只是通信第一环。

**Docker Compose**：用声明式文件编排多个 Docker 服务、网络和卷。

**DTO**：接口输入/输出的数据对象，用于隔离 HTTP 契约与数据库实体。

**Flyway**：按版本管理并执行数据库迁移的工具。

**Grafana**：查询多种数据源并构建仪表盘和探索视图的平台。

**Health check**：判断进程或服务某一层可用性的检查，需明确检查语义。

**Helm**：Kubernetes 应用包和模板管理工具，通过 Chart 与 values 管理差异。

**HPA**：根据指标调整工作负载副本数的水平自动扩缩容器。

**HTTP status**：HTTP 响应结果分类。状态码要结合响应体和业务语义解释。

**Idempotency**：同一业务意图重复执行不会产生额外副作用。通常需要稳定键和持久化结果。

**Image**：容器只读模板，由分层文件系统和元数据组成。

**Ingress**：Kubernetes 中描述 HTTP/HTTPS 入口路由的资源，需要控制器实现。

**JPA/Hibernate**：Java 持久化规范与常用实现，负责对象关系映射和数据库交互。

**Kubernetes Event**：集群组件记录的近期资源事件，是调度、拉镜像、挂载和探针故障的重要证据。

**Liveness probe**：判断容器是否需要重启的探针。

**Loki**：以标签索引并存储日志的系统，常与 Grafana 配合查询。

**Micrometer**：Java 指标门面，Spring Boot 可通过它导出 Prometheus 格式指标。

**Modular monolith**：单一部署单元内部按业务边界模块化，不等于没有架构。

**NAT**：在网络边界转换地址/端口，Docker 端口映射是常见应用之一。

**NetworkPolicy**：Kubernetes 对 Pod 流量的策略声明，依赖 CNI 支持。

**Nginx**：可提供静态文件、反向代理、TLS 终止和访问日志的 Web/代理服务器。

**Observability**：通过指标、日志、追踪等外部信号理解系统内部状态的能力。

**PDB**：PodDisruptionBudget，限制自愿中断期间可同时不可用的 Pod 数量。

**PID 1**：容器内第一个进程，负责容器生命周期并需正确处理信号和子进程。

**Pod**：Kubernetes 最小调度单元，内部容器共享网络命名空间和部分卷。

**Prometheus**：通过抓取保存时间序列、执行 PromQL 和评估告警规则的监控系统。

**PVC**：PersistentVolumeClaim，工作负载对持久存储的申请。

**Readiness probe**：判断实例是否应接收 Service 流量的探针。

**Repository**：封装持久化访问的代码层，不等同于 Git 仓库。

**REST**：以资源和统一 HTTP 语义组织接口的一种架构风格。

**RPO/RTO**：最大可接受数据丢失窗口与最大可接受恢复时长。

**Service**：Kubernetes 为一组 Pod 提供稳定访问和服务发现的资源。

**SLA/SLO/SLI**：对外承诺、内部可靠性目标和实际测量指标，三者不能混用。

**StatefulSet**：为有状态应用提供稳定身份和卷绑定的控制器。

**systemd**：Linux 常见系统与服务管理器。

**Transaction**：数据库中的一组原子操作，有明确提交和回滚边界。

**Volume**：独立于容器可写层的数据存储挂载；持久化不等于备份。

<a id="chapter-73"></a>

## 第 73 章　最终验收清单

### 73.1 代码与版本

- [ ] Git 仓库已有清晰提交历史，不再是“全部未跟踪文件”；
- [ ] README 能在新机器指导启动；
- [ ] 后端、前端、数据库迁移和部署配置版本对应；
- [ ] 已执行迁移不被修改，新变更使用 V2+；
- [ ] 密钥、构建产物、IDE 文件和本地数据未误提交；
- [ ] 存在一个可演示版本标签和变更记录。

### 73.2 Java 与 MySQL

- [ ] 能画出转账调用链并找到实际代码；
- [ ] 能解释三张核心表和索引用途；
- [ ] 重复请求不重复扣款；
- [ ] 同键不同请求得到冲突；
- [ ] 余额不足完整回滚；
- [ ] 成功转账有两条配对流水；
- [ ] 集成测试使用真实 MySQL；
- [ ] 已增加至少一个本人独立完成的功能和测试；
- [ ] 并发测试未完成时不写高并发。

### 73.3 前端

- [ ] 桌面和窄屏可用；
- [ ] 关键页面使用真实 API 与 MySQL 数据；
- [ ] 加载、空、成功、失败状态清楚；
- [ ] 直接刷新子路由不 404；
- [ ] 转账重复提交受到控制；
- [ ] 浏览器控制台没有关键错误；
- [ ] 能从 Network 找到状态码、请求体和关联 ID。

### 73.4 Docker Compose

- [ ] 新环境能按 README 构建；
- [ ] 所有服务状态和健康符合预期；
- [ ] 端口表准确；
- [ ] 重建应用容器不丢 MySQL 数据；
- [ ] 默认停止流程不删除卷；
- [ ] 镜像为多阶段构建并以非 root 运行；
- [ ] 完成应用停止与数据库停止故障演练。

### 73.5 Linux

- [ ] 环境检查包含系统、资源、时间、网络、端口和依赖；
- [ ] 应用由服务账户运行；
- [ ] systemd 可启动、停止、自启、查状态和日志；
- [ ] Nginx 静态资源、SPA 路由和 API 代理正常；
- [ ] 配置、程序、日志和数据目录分离；
- [ ] 发布后有健康门禁和冒烟测试；
- [ ] 完成一次不可变发布和回滚。

### 73.6 观测与告警

- [ ] Prometheus 所有预期 Targets 为 UP；
- [ ] 能解释并执行至少三条 PromQL；
- [ ] Grafana 能展示请求、JVM、容器/主机趋势；
- [ ] Loki 能按服务和关联 ID 查日志；
- [ ] Alertmanager 能观察告警生命周期；
- [ ] 明确本地空接收器不等于外部通知；
- [ ] 至少一次告警有触发、恢复和证据记录；
- [ ] readiness 是否包含数据库已用实际配置/端点验证。

### 73.7 Kubernetes

- [ ] 当前 context 与 namespace 清楚；
- [ ] Deployment、Service、StatefulSet/PVC 状态正常；
- [ ] 探针语义和结果经过验证；
- [ ] 删除 Pod 后控制器完成自愈；
- [ ] 错误镜像能通过 Events 定位；
- [ ] HPA 有 metrics-server 与真实指标证据，或明确记录未验证；
- [ ] PDB、RBAC、NetworkPolicy 的边界能解释；
- [ ] Helm 安装、升级、回滚至少完成一次；
- [ ] 不把单机 Minikube 说成生产高可用。

### 73.8 备份与恢复

- [ ] 有独立备份文件和校验值；
- [ ] 在独立恢复库完成过恢复；
- [ ] 验证账户、订单和流水关联；
- [ ] 记录练习 RPO、RTO 与实测恢复耗时；
- [ ] 明确容器卷/PVC 不等于备份。

### 73.9 演示与面试

- [ ] 30 秒、2 分钟、5 分钟版本均能脱稿讲；
- [ ] 20 分钟演示可稳定完成；
- [ ] 有架构图、正常截图和备用录屏；
- [ ] 能回答至少 40 个追问并举实际证据；
- [ ] 能说出当前项目至少五个真实缺口；
- [ ] AI 参与范围如实说明；
- [ ] 简历每条陈述都能指向 E2 或 E3 证据。

<a id="chapter-74"></a>

## 第 74 章　资料索引与继续学习

### 74.1 项目内部资料

学习时优先阅读实际仓库，而不是把本手册当成代码事实的替代品：

- 根目录 README：入口、架构、启动与范围；
- `backend`：Java 业务代码、配置、Flyway 和测试；
- `frontend`：React 页面、API 客户端和状态处理；
- `deploy/compose`：本地完整编排；
- `deploy/linux`：systemd、Nginx 与部署脚本；
- `deploy/k8s` 与 `deploy/helm`：Kubernetes 清单和 Chart；
- `observability`：Prometheus、Grafana、Loki、Alloy、Alertmanager；
- `scripts`：启动、验收、冒烟、备份恢复和发布辅助；
- `docs`：架构、操作手册、验收记录和 UI 证据。

文件可能随项目演进而变化。每次学习都以当前仓库和实际运行结果为准，历史验收报告只能作为检查清单，不能直接代表今天仍健康。

### 74.2 官方资料路径

遇到版本相关行为时，优先查项目所用版本的官方文档：Spring Boot Reference、MySQL Reference Manual、Docker Docs、Kubernetes Documentation、Prometheus Docs、Grafana/Loki Docs、Nginx Documentation、Rocky Linux/Red Hat systemd 管理资料。社区文章可以帮助理解，但配置语义和安全边界应回到官方来源验证。

### 74.3 如何阅读官方文档

先带着具体问题搜索，例如“Spring Boot readiness health group include database”，不要从首页无目的通读。记录版本、默认值、示例适用条件和与你当前配置的差异。实验后把结果写回项目手册或故障记录。

### 74.4 手册维护规则

本手册描述的是一个持续演进项目。每次新增功能或部署能力，都同步更新：架构图、端口表、配置来源、健康/监控、故障场景、验收证据、简历边界。删除功能时也要删掉过期表述。

版本维护建议：文档首页记录版本和日期；重大章节变更进入 Git；命令示例在目标环境复测；所有“已完成”陈述都附最近验证时间。这样文档才是活的知识库，而不是一次性生成的厚文件。

## 结语：项目的终点不是“代码很多”

当你能从浏览器的一次失败请求出发，判断它经过的网络和软件层，找到日志与指标证据，检查数据库一致性，安全恢复服务，并解释为什么采取这些步骤时，OpsPilot 才真正成为你的项目。

厚文档只能提供地图。真正的能力来自你按地图走过：亲手启动、亲手修改、亲手制造失败、亲手恢复、留下证据，然后用自己的话讲明白。每完成一个实训，就把对应简历能力从“了解”升级为“熟悉”；只有经过多次独立处理和迁移应用，才谨慎使用“熟练”。
