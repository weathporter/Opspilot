# NorthLedger 三节点微服务发布与验收手册

> 状态：仓库交付手册。以下命令要由操作者在自己的三台虚拟机和受保护的发布机上执行；未执行前，不能把它写成“已在三节点部署成功”。旧 Compose 单体和旧数据库不会被本流程自动删除或覆盖。

## 0. 先理解最终链路

```text
浏览器 → Ingress → Web/Nginx → Access（登录、Redis Session、CSRF、RBAC）
                                      ├→ Ledger（账户、转账、流水；本地 MySQL 事务）
                                      └→ Operations（总览、对账历史）→ Ledger 只读接口

PR → GitHub Actions 测试/构建/Helm 校验
main 合并 → 同一提交 SHA 的四个 GHCR 镜像 → 受保护环境人工放行
        → 自托管 Windows runner → Helm 原子升级 → 同源冒烟 → 失败回滚
```

三节点是 `nl-cp1`、`nl-worker1`、`nl-worker2`。Access、Ledger、Operations、Web 分别是独立 Deployment/Service，默认各两副本。MySQL 固定在 worker1，Redis 固定在 worker2，均为单副本 StatefulSet。**这是有真实服务拆分和 CI/CD 的实验集群，不是高可用生产基础设施。**单控制平面、单机数据库与本地盘都是单点。

## 1. 集群先决条件（操作者执行）

建议给三个 VM 各分配 4 GiB 内存，合计 12 GiB；截图里约 1.6 GiB 的节点不足以承载本项目，也可能过不了 `kubeadm init` 最低内存检查。所有节点应有固定 IP、不同主机名、无 swap、运行中的 containerd/kubelet，以及相互可达的节点网络。至少具备：

1. `kubectl get nodes -o wide` 显示 **3 个 Ready**；`kubectl -n kube-system get pods -o wide` 的 DNS 与 CNI Pod 正常。
2. CNI 能执行 Kubernetes NetworkPolicy。若当前插件忽略策略，Chart 中的网络隔离只是声明，不是实际安全边界；先通过拒绝/放行实验确认，再决定是否更换插件。
3. 已安装可工作的 Ingress Controller，`kubectl get ingressclass` 能看到与 `values.yaml` 的 `ingress.className` 一致的 `nginx`。没有它仍可用端口转发验收，但不能说 Ingress 入口已打通。
4. 管理机能访问 `ghcr.io`，工作节点能拉取四个镜像；私有包需设置拉取凭据。
5. 控制平面或管理机有 Helm 3、kubectl 和可用 kubeconfig。不要把管理员 kubeconfig 放进仓库。

在 `nl-cp1` 上核查：

```bash
kubectl get nodes -o wide
kubectl -n kube-system get pods -o wide
kubectl get ingressclass
kubectl get storageclass
```

任一节点不 Ready、DNS 不通或镜像无法拉取时先停止部署，使用 `kubectl describe node`、`kubectl -n kube-system describe pod` 和 `journalctl -u kubelet -xe` 看事件与日志，不要使用 `--ignore-preflight-errors` 掩盖资源不足。

## 2. 静态存储（在对应 VM 执行）

本项目使用静态 Local PV；PVC 不会自动创建宿主机目录。先核对 `kubectl get node --show-labels` 中的 `kubernetes.io/hostname` 确实为 `nl-worker1` 和 `nl-worker2`，否则修改 [PV 声明](../../deploy/k8s/storage/three-node-local-pv.yaml) 后再应用。

在 `nl-worker1`：

```bash
sudo install -d -o 999 -g 999 -m 0700 /var/lib/northledger/mysql
sudo stat -c '%U:%G %a %n' /var/lib/northledger/mysql
```

在 `nl-worker2`：

```bash
sudo install -d -o 999 -g 1000 -m 0700 /var/lib/northledger/redis
sudo stat -c '%U:%G %a %n' /var/lib/northledger/redis
```

SELinux 若处于 Enforcing 且 Pod 报 `permission denied`，先检查 `ausearch -m AVC -ts recent`、`ls -Zd /var/lib/northledger/*`；在确认是目录标签问题后为相应目录设置容器可访问标签，再 `restorecon -Rv`，不要直接关 SELinux。因为 Local PV 与节点绑定，worker 故障不会自动把数据复制到另一台。

在 `nl-cp1`，从仓库根目录执行：

```bash
kubectl apply -f deploy/k8s/storage/three-node-local-pv.yaml
kubectl get pv,storageclass
kubectl create namespace northledger
```

若 namespace 已存在，`kubectl create` 会提示 AlreadyExists；核对目标后继续即可。**不要删除已有 PVC/PV 来“重试”**，Retain 卷重绑和数据库恢复必须先查清数据状态。

## 3. 一次性运行 Secret

三个数据库用户密码只接受 24–128 位 ASCII 字母、数字、下划线或连字符，这是 MySQL 首次初始化脚本的约束。服务令牌必须彼此不同；Redis、MySQL root、管理员密码也不要复用。下面的输入不会写进命令历史，但会短暂落在权限为 600 的临时文件，操作完成后立即擦除；请在可信的 `nl-cp1` 终端输入并妥善保存原值。

```bash
umask 077
nl_secret_file=$(mktemp)
trap 'shred -u "$nl_secret_file" 2>/dev/null || rm -f "$nl_secret_file"' EXIT
nl_add_secret() {
  local nl_key="$1" nl_value
  read -r -s -p "${nl_key}: " nl_value
  printf '\n'
  printf '%s=%s\n' "$nl_key" "$nl_value" >> "$nl_secret_file"
  unset nl_value
}
nl_add_secret mysql-root-password
nl_add_secret access-db-password
nl_add_secret ledger-db-password
nl_add_secret operations-db-password
nl_add_secret redis-password
nl_add_secret internal-access-token
nl_add_secret internal-operations-token
nl_add_secret internal-operations-read-token
nl_add_secret bootstrap-admin-username
nl_add_secret bootstrap-admin-password
kubectl -n northledger create secret generic northledger-runtime \
  --from-env-file="$nl_secret_file" --dry-run=client -o yaml | kubectl apply -f -
shred -u "$nl_secret_file"
trap - EXIT
kubectl -n northledger describe secret northledger-runtime
```

最后一条只显示键名和字节数，检查 10 个键齐全，**不要** `kubectl get secret -o yaml` 发到聊天、Issue 或日志。Secret 默认不是加密保险箱；限制 kubeconfig 与 namespace RBAC 仍是必要条件。MySQL 的 `/docker-entrypoint-initdb.d` 只在全新空数据目录执行；换 Secret 并不会自动改已有数据库用户密码。

若 GHCR 包是私有的，先在 `northledger` namespace 创建名为 `ghcr-pull` 的 registry Secret，并在自己的未提交 values 覆盖文件中写 `global.imagePullSecrets: [{name: ghcr-pull}]`。公开包则保持空列表。不要把 PAT 放入仓库或 Helm values。自动发布时，Windows runner 必须能读取同一份非敏感 values 覆盖文件；在 GitHub Environment Variable `NORTHLEDGER_VALUES_FILE` 中填写其绝对路径（不是文件内容）。

## 4. 镜像和首次安装

四个镜像名分别为 `ghcr.io/weathporter/opspilot-{access,ledger,operations,web}:<完整40位提交SHA>`。只有 main 上全部 CI 通过后才会发布；PR 只构建、不发布。先在 GitHub Packages 或 `docker manifest inspect` 确认四个同一 SHA 都存在，再在控制平面执行：

```bash
nl_sha='<替换为已发布的40位小写提交SHA>'
helm lint deploy/k8s/helm/northledger-microservices
helm template northledger deploy/k8s/helm/northledger-microservices \
  -n northledger --set-string global.imageTag="$nl_sha" >/dev/null
helm upgrade --install northledger deploy/k8s/helm/northledger-microservices \
  -n northledger --atomic --wait --timeout 10m \
  --set-string global.imageTag="$nl_sha" --set-string global.version="$nl_sha"
kubectl -n northledger get pods,svc,pvc,ingress -o wide
kubectl -n northledger rollout status deploy/northledger-access
kubectl -n northledger rollout status deploy/northledger-ledger
kubectl -n northledger rollout status deploy/northledger-operations
kubectl -n northledger rollout status deploy/northledger-web
```

若使用私有 GHCR 包或 TLS，把非敏感覆盖配置放进自己的 `values-lab.local.yaml`（仓库中不提交），命令加 `-f values-lab.local.yaml`。HTTP 实验默认 `sessionCookieSecure=false`；使用 HTTPS Ingress 时设置 TLS Secret 且改为 `true`，否则浏览器会话可能无法按预期保持。Ingress 域名默认 `northledger.local`，需把它解析到实际 Ingress 入口；在这之前可用端口转发：

```bash
kubectl -n northledger port-forward svc/northledger-web 18000:8080 --address 127.0.0.1
```

这条命令只在运行它的机器上开放 `http://127.0.0.1:18000`。若在 VM 的 FinalShell 中运行，而浏览器在 Windows 宿主机上，需使用 FinalShell 的 SSH 本地端口转发，或在 Windows 管理机用 kubeconfig 执行端口转发；不要为了省事把管理接口监听到 `0.0.0.0`。

## 5. CI/CD 的受保护发布机

GitHub Actions 的 `verify` 保留旧单体回归；`verify-microservices` 对三个服务分别跑真实 MySQL/Redis 测试，`verify-microservices-chart` 渲染 Chart，`build-microservice-images` 在 PR 验证四个镜像可构建。main 全绿后 `publish-images` 才将同一提交 SHA 发布到 GHCR。`deploy-three-node-lab` 只在设置好 `three-node-lab` Environment 审核人与自托管 Windows runner 后执行。

仓库管理员需要完成：保护 main（PR 与必需检查），创建 `three-node-lab` Environment 并指定审核人；在可访问三节点 API 的 Windows 机器安装并注册自托管 runner，添加 `northledger-deploy` 标签，安装 Helm/kubectl，限制该机器与 runner 账号的使用范围。Environment Secrets 至少包含 `KUBECONFIG_B64`、`SMOKE_USERNAME`、`SMOKE_PASSWORD`；需要私有镜像拉取或 TLS 配置时设置 `NORTHLEDGER_VALUES_FILE` Environment Variable。kubeconfig 要使用**仅限 northledger 发布所需权限**的身份，不能长期使用整个集群的 admin.conf；授权细节需按实际 Ingress、Secret 与 Helm Release 权限核验。

本机生成 `KUBECONFIG_B64` 可在 PowerShell 将内容直接复制到剪贴板，不输出到终端，然后粘贴进 GitHub Environment Secret：

```powershell
$nlKubeconfig = 'C:\path\to\restricted-northledger-kubeconfig'
[Convert]::ToBase64String([IO.File]::ReadAllBytes($nlKubeconfig)) | Set-Clipboard
```

粘贴后清空剪贴板，并确认未在历史记录、截图或聊天中泄露。发布脚本只接受完整 SHA，`helm upgrade --atomic --wait` 失败时 Helm 处理升级回滚；升级成功但同源冒烟失败时脚本回滚到先前修订。首次安装没有旧修订可回退，脚本保留现场供诊断并报告失败。

## 6. 验收必须留下的证据

1. `kubectl get nodes -o wide`：3 Ready、节点 IP/角色正确。
2. `kubectl -n northledger get deploy,sts,pod,pvc,svc,ingress -o wide`：四个 Deployment 各两副本，MySQL/Redis 各一副本，PVC Bound；Access/Ledger/Operations 不经 Ingress 对外暴露。
3. `helm -n northledger history northledger`：记录发布 SHA 与修订号；`kubectl -n northledger get pod -o jsonpath='{..image}'` 核对四个应用镜像 SHA。
4. 用受保护账号运行 `scripts/k8s/smoke-microservices.ps1`：实际走 Web → Access → Ledger/Operations 的登录、会话、CSRF、401、账户只读、总览、一次对账写入和读回。脚本不会替用户创建资金记录。
5. 人工用测试账户验证开户、转账、同一请求号重试只扣款一次、双边流水金额与账户一致；备份 MySQL，恢复到**另一套隔离数据库**后核对行数。未经核对不要在唯一实例上做破坏性恢复演练。
6. 故障演练至少包含一个无状态 Pod 重建、一个错误镜像发布与回滚、Redis 故障后会话/总览的实际表现、worker1 存储故障的单点限制。保存事件、日志、时间线与恢复结果，不能把静态 YAML 当成演练证据。

## 7. 常见失败的第一响应

| 表现 | 先看什么 | 常见处理方向 |
| --- | --- | --- |
| `Pending`、PVC 不 Bound | `kubectl describe pvc/pod`、PV 节点亲和性、`kubectl get nodes --show-labels` | 核实宿主目录、主机名、容量与 storageClass；保留原数据，不删卷试错 |
| `ImagePullBackOff` | `kubectl describe pod` 的 Events、GHCR 四个 SHA 是否存在 | 核查网络、包可见性、私有拉取 Secret、镜像名与大小写 |
| MySQL `CrashLoopBackOff` | `kubectl logs`、PVC、宿主目录权限、SELinux AVC | 先区分空目录初始化失败和旧目录用户密码不匹配，勿格式化 PV |
| Access 未就绪 | Access 日志与 `/actuator/health/readiness`，MySQL/Redis Service | 查 Secret 键名、DNS、网络策略、数据库迁移；不要关闭就绪门禁 |
| 网页 502/503 | Web、Access、下游 Pod/Service/Endpoints，带 traceId 的日志 | 找出实际失败跳点；Ledger 不可用时 Operations 不会伪造看板数据 |
| Ingress 不通但端口转发可用 | IngressClass、Controller、域名、TLS、Controller Events | 入口层问题，与服务本体分开定位 |
| 部署后登录失败 | CSRF Cookie/Header、Redis、`SESSION_COOKIE_SECURE`、域名与协议 | HTTPS 下启用安全 Cookie；不要把内部令牌暴露到浏览器 |

结束排查时记录：触发条件 → 观察到的证据 → 根因 → 处理 → 复测 → 防复发措施。

## 8. 旧单体数据迁移与备份边界

新三服务使用三个全新数据库；**Chart 不会读取或搬迁旧单体 `opspilot` 库**。若只是学习环境，可以在旧库备份后用全新空库演示。若要保留旧数据，先停写旧入口、做完整逻辑备份并在隔离实例演练恢复，再将 `app_user/app_user_role/audit_event` 导入 Access 库，将 `account/transfer_order/ledger_entry` 导入 Ledger 库；不要复制旧库的 `flyway_schema_history` 或 `SPRING_SESSION*`。目标库先让各服务 Flyway 建表，再对照字段、外键与 ID 做数据导入及行数、余额和双边流水核对。Operations 的对账历史从新库开始。正式切流前执行一次只读核验并保留旧入口回退窗口；有写入的回退需要另做数据同步方案，不能简单 Helm rollback。

现有项目的 MySQL 备份恢复基础流程见 [MySQL 手册](mysql-backup-restore.md)。本手册没有自动执行数据迁移，也没有声称旧数据已搬迁。

参考官方文档：[Kubernetes 本地卷](https://kubernetes.io/docs/concepts/storage/volumes/#local)、[NetworkPolicy](https://kubernetes.io/docs/concepts/services-networking/network-policies/)、[GitHub 自托管 runner](https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/add-runners)、[Helm 升级与回滚](https://helm.sh/docs/helm/helm_upgrade/) 。
