# OpsPilot 三节点虚拟机部署实战

本目录把 OpsPilot 实际部署到此前搭建的三节点实验集群：

```text
k8s-master  192.168.99.100  Kubernetes v1.21 控制平面
k8s-node1   192.168.99.101  Web/API + MySQL 本地卷
k8s-node2   192.168.99.102  Web/API
```

这是一套旧版本兼容实验，不是生产环境。Kubernetes v1.21 已停止维护，Docker dockershim 在 v1.24 被移除；项目的现代交付仍保留在 `deploy/k8s/helm/opspilot`。

## 最终资源

| 资源 | 数量 | 作用 |
| --- | ---: | --- |
| `opspilot-web` Deployment | 2 | React/Nginx 前端，尽量分布到两个 worker |
| `opspilot-api` Deployment | 2 | Spring Boot API，尽量分布到两个 worker |
| `mysql` StatefulSet | 1 | 学习用数据库，不代表高可用 |
| `opspilot-web` NodePort | 30080 | 从 Windows 浏览器访问 |
| Local PV/PVC | 5 GiB | 把 MySQL 数据保存在 node1 `/data/opspilot/mysql` |
| PDB | Web/API 各 1 个 | 节点主动维护时至少保留一个副本 |
| NetworkPolicy | 4 个 | Web → API → MySQL 逐段放行 |

## 1. Windows：测试并构建镜像

在 PowerShell 中进入项目：

```powershell
Set-Location D:\Develop\OpsPilot

mvn -B -ntp test
npm --prefix frontend ci
npm --prefix frontend run build

docker build --build-arg APP_VERSION=0.1.1 -t opspilot-api:0.1.1 .
docker build -f frontend/Dockerfile.runtime -t opspilot-web:0.1.1 .
docker pull mysql:8.4

New-Item -ItemType Directory -Force D:\Develop\OpsPilot\dist\k8s-vm-lab
docker save --output D:\Develop\OpsPilot\dist\k8s-vm-lab\opspilot-images-0.1.1.tar opspilot-api:0.1.1 opspilot-web:0.1.1 mysql:8.4
Get-FileHash D:\Develop\OpsPilot\dist\k8s-vm-lab\opspilot-images-0.1.1.tar -Algorithm SHA256
```

前三条命令分别验证 Java、安装前端锁定依赖、构建前端产物；任何一步失败都先停下排障，不能使用跳过测试的包继续部署。

## 2. FinalShell：把镜像导入两个 worker

使用 FinalShell 把下面的文件分别上传到 node1、node2 的 `/root/opspilot/`：

```text
D:\Develop\OpsPilot\dist\k8s-vm-lab\opspilot-images-0.1.1.tar
```

在 node1 和 node2 分别执行：

```bash
cd /root/opspilot
sha256sum opspilot-images-0.1.1.tar
docker load -i opspilot-images-0.1.1.tar
docker image inspect opspilot-api:0.1.1 --format '{{.Id}}'
docker image inspect opspilot-web:0.1.1 --format '{{.Id}}'
docker image inspect mysql:8.4 --format '{{.Id}}'
```

两台 worker 的 tar SHA-256 和三个镜像 ID 必须分别一致。Kubernetes 使用 `IfNotPresent`，因此会使用导入到节点 Docker 中的镜像。

## 3. node1：准备 MySQL 本地数据目录

只在 node1 执行：

```bash
mkdir -p /data/opspilot/mysql
chown -R 999:999 /data/opspilot/mysql
chmod 700 /data/opspilot/mysql
ls -ld /data/opspilot/mysql
```

PV 使用 local volume 并通过 nodeAffinity 固定到 `k8s-node1`。这能练习持久化，但 node1 故障时 MySQL 不能自动迁移到 node2。

## 4. master：上传部署文件并执行预检

用 FinalShell 上传：

```text
D:\Develop\OpsPilot\deploy\k8s\vm-lab
D:\Develop\OpsPilot\scripts\k8s\preflight-vm-lab.sh
D:\Develop\OpsPilot\scripts\k8s\deploy-vm-lab.sh
```

建议在 master 形成下面的路径：

```text
/root/opspilot-deploy/
├── vm-lab/
├── preflight-vm-lab.sh
└── deploy-vm-lab.sh
```

然后执行：

```bash
cd /root/opspilot-deploy
sed -i 's/\r$//' preflight-vm-lab.sh deploy-vm-lab.sh
chmod +x preflight-vm-lab.sh deploy-vm-lab.sh
bash -n preflight-vm-lab.sh deploy-vm-lab.sh
./preflight-vm-lab.sh
```

预检只读，并且必须输出 `PASS`。如果版本或运行时不一致，不继续执行后面的安装命令。

## 5. master：限定业务只运行在两个 worker

```bash
kubectl label node k8s-node1 opspilot.io/workload=true --overwrite
kubectl label node k8s-node2 opspilot.io/workload=true --overwrite
kubectl get nodes -L opspilot.io/workload
```

Deployment 使用该标签选择 node1/node2，不依赖 master 是否存在污点。

## 6. master：创建实验 Secret

先创建命名空间，再通过隐藏输入读取密码。密码不会写入项目文件，shell 历史只记录变量名：

```bash
kubectl apply -f /root/opspilot-deploy/vm-lab/namespace.yaml

read -rsp 'OpsPilot DB password: ' OPSPILOT_DB_PASSWORD; echo
read -rsp 'MySQL root password: ' OPSPILOT_ROOT_PASSWORD; echo

kubectl -n opspilot create secret generic opspilot-database \
  --from-literal=database-password="${OPSPILOT_DB_PASSWORD}" \
  --from-literal=mysql-root-password="${OPSPILOT_ROOT_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -

unset OPSPILOT_DB_PASSWORD OPSPILOT_ROOT_PASSWORD
kubectl -n opspilot get secret opspilot-database
```

最后一条命令只检查 Secret 存在，不显示内容。

## 7. master：安装 OpsPilot

```bash
cd /root/opspilot-deploy
./deploy-vm-lab.sh /root/opspilot-deploy/vm-lab
```

部署脚本会依次等待 MySQL、API、Web 就绪。成功后检查：

```bash
kubectl -n opspilot get pods -o wide
kubectl -n opspilot get svc
kubectl -n opspilot get endpoints
kubectl -n opspilot get pvc
kubectl get pv opspilot-mysql-pv
kubectl -n opspilot get events --sort-by=.lastTimestamp
```

预期：

- 两个 API Pod 和两个 Web Pod 分布在 node1、node2。
- `mysql-0` 固定在 node1。
- PVC/PV 为 `Bound`。
- Web Service 的 NodePort 为 `30080`。

## 8. 浏览器和业务验收

Windows 浏览器访问任意一个地址：

```text
http://192.168.99.101:30080
http://192.168.99.102:30080
```

先从 master 验证健康链路：

```bash
curl -i http://192.168.99.101:30080/health
curl -i http://192.168.99.102:30080/health
```

两次都应返回 HTTP 200 和 `{"status":"UP"}`。随后在页面中创建两个账户并完成一次转账；再检查 API 日志：

```bash
kubectl -n opspilot logs deployment/opspilot-api --tail=100
```

## 9. 第一组 Kubernetes 实战

### Pod 自愈

```bash
kubectl -n opspilot get pods -l app.kubernetes.io/component=api -o wide
API_POD_ON_NODE1="$(kubectl -n opspilot get pods -l app.kubernetes.io/component=api --field-selector spec.nodeName=k8s-node1 -o jsonpath='{.items[0].metadata.name}')"
kubectl -n opspilot delete pod "${API_POD_ON_NODE1}"
kubectl -n opspilot get pods -l app.kubernetes.io/component=api -o wide --watch
```

观察旧 Pod 消失、Deployment 创建新 Pod、Service 地址保持不变。

### 扩缩容

```bash
kubectl -n opspilot scale deployment opspilot-api --replicas=3
kubectl -n opspilot rollout status deployment/opspilot-api
kubectl -n opspilot get pods -l app.kubernetes.io/component=api -o wide
kubectl -n opspilot scale deployment opspilot-api --replicas=2
```

### 滚动发布与回滚

先在 Windows 构建 `0.1.2`，把镜像导入两个 worker，然后执行：

```bash
kubectl -n opspilot set image deployment/opspilot-api api=opspilot-api:0.1.2
kubectl -n opspilot annotate deployment/opspilot-api kubernetes.io/change-cause='upgrade api image to 0.1.2' --overwrite
kubectl -n opspilot rollout status deployment/opspilot-api
kubectl -n opspilot rollout history deployment/opspilot-api
kubectl -n opspilot rollout undo deployment/opspilot-api
kubectl -n opspilot rollout status deployment/opspilot-api
kubectl -n opspilot get deployment opspilot-api -o jsonpath='{.spec.template.spec.containers[?(@.name=="api")].image}{"\n"}'
```

这组命令只更新一个 Deployment 的镜像，因此回滚后检查 Pod 镜像和真实业务请求，不能只看 `rollout status`。`APP_VERSION` 仍由 ConfigMap 管理，不会被 `kubectl rollout undo` 一起回滚；后续 Helm 实战会把镜像与配置作为同一 Release 管理。

## 10. 排障顺序

```bash
kubectl -n opspilot get pods -o wide
kubectl -n opspilot get events --sort-by=.lastTimestamp
kubectl -n opspilot describe pod POD_NAME
kubectl -n opspilot logs POD_NAME -c CONTAINER_NAME --tail=100
kubectl -n opspilot get svc,endpoints
kubectl -n opspilot get pvc
```

坚持按“状态 → Events → describe → 日志 → Service/Endpoints → 存储”排查，不先重启、不先改 YAML。

## 官方依据

- NodePort 从每个节点 IP 的静态端口暴露 Service：<https://kubernetes.io/docs/concepts/services-networking/service/#type-nodeport>
- local volume 必须配合 PersistentVolume 的 nodeAffinity：<https://kubernetes.io/docs/concepts/storage/volumes/#local>
- topology spread 用节点拓扑域控制副本分布：<https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/>
- startup/readiness/liveness 的职责区别：<https://kubernetes.io/docs/concepts/workloads/pods/probes/>
- dockershim 在 Kubernetes v1.24 被移除：<https://kubernetes.io/blog/2022/02/17/dockershim-faq/>
