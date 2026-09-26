# NorthLedger：CentOS Stream 9 三节点 Kubernetes 完整实施与运维手册

> 当前内存受限主线是[两节点 VMware 创建与复用](two-node-vmware-creation.md)和[两节点微服务发布](two-node-microservices-release.md)。本文件保留三节点扩展流程；表中的 2048 MiB 控制平面仅是历史实验配置，实际已出现 1674 MiB 预检失败，不能把它当作当前两节点推荐值。两节点先给控制平面 3 GiB、worker 5 GiB，并核对 `ip_forward=1`。

> 目标：由学习者亲自在 VMware 和 FinalShell 中完成三台 Linux 节点配置，构建可验证的 kubeadm Kubernetes 集群，并为 NorthLedger 的 Helm 发布、存储、监控、回滚和故障演练提供统一操作入口。
> 版本：CentOS Stream 9、Kubernetes 1.36.4、containerd 2.x、Calico 3.32.2、Helm 3。
> 边界：这是单台笔记本上的三节点工程实战环境，不是控制平面高可用、数据库高可用或跨机房容灾环境。文档会明确区分“真实完成的能力”和“受实验环境限制的边界”。

## 0. 使用方法与执行纪律

本文是执行手册，不是命令清单。每一阶段均按以下顺序操作：

1. 确认标题标注的执行位置，是三台节点、仅控制平面、仅工作节点，还是 Windows PowerShell；
2. 一次只执行一个代码块，看到预期结果后再进入下一块；
3. 命令失败时停止，不继续粘贴后续命令；
4. 保存 `kubeadm init`、`kubeadm join`、Helm 发布和故障演练的输出，但不要把令牌、密码或 kubeconfig 提交到 GitHub；
5. 文中的“成功”只以命令输出为准，进程存在、端口监听或 HTTP 200 都不能单独证明业务完整可用。

FinalShell 粘贴时尤其注意：反斜杠 `\` 表示命令尚未结束。看到终端提示符变成 `>` 时，必须把剩余行粘贴完成；不要把文档中的“预期输出”也粘贴进终端。

## 1. 为什么不用 CentOS 7

CentOS 7 已停止维护，内核、软件仓库和现代容器运行时都不适合作为 2026 年新项目的长期底座。本手册中的 `dnf`、Kubernetes 1.36 和 Calico 3.32 命令按 **CentOS Stream 9** 编写，不能原样用于 CentOS 7。

旧 CentOS 7 虚拟机可以保留，用于比较旧系统服务管理、仓库失效和兼容性排障，但简历项目的主部署证据来自新环境。

## 2. 节点规划

| 节点 | 主机名 | IP | 角色 | vCPU | 内存 | 磁盘 |
| --- | --- | --- | --- | ---: | ---: | ---: |
| 控制平面 | `nl-cp1` | `192.168.66.110/24` | API Server、etcd、调度与控制器 | 2 | 2048 MiB | 40 GiB |
| 工作节点 1 | `nl-worker1` | `192.168.66.111/24` | 业务、MySQL、部分监控 | 2 | 3584 MiB | 40 GiB |
| 工作节点 2 | `nl-worker2` | `192.168.66.112/24` | 业务、Redis、部分监控 | 2 | 3584 MiB | 40 GiB |

网络规划：

| 用途 | 网段/地址 |
| --- | --- |
| VMware VMnet8 NAT | `192.168.66.0/24` |
| VMware NAT 网关 | `192.168.66.2` |
| Windows VMnet8 地址 | `192.168.66.1` |
| VMware DHCP 地址池 | `192.168.66.128-192.168.66.254` |
| Kubernetes Pod CIDR | `10.244.0.0/16` |
| Kubernetes Service CIDR | `10.96.0.0/12` |

三个网段互不重叠。`.110-.112` 位于 VMware DHCP 地址池之外，避免固定地址与 DHCP 租约冲突。

## 3. VMware 中每台虚拟机的设置

三台虚拟机均设置：

1. 操作系统：CentOS Stream 9 Minimal；
2. Network Adapter：`NAT`；
3. 磁盘：40 GiB，精简置备；
4. CD/DVD：系统安装完成后断开 ISO；
5. 每台机器必须有不同的生成型 MAC 地址；
6. 用户名建议统一为 `opsadmin`，勾选管理员权限；
7. 不要在仓库、截图或聊天里保存密码。

完成安装后，先分别登录虚拟机控制台，再执行对应节点的网络配置。

## 4. 配置静态 IP 和主机名

### 4.1 先找出网卡连接名

三台机器都执行：

```bash
nmcli -t -f NAME,DEVICE connection show --active
```

预期能看到类似：

```text
ens33:ens33
```

下面的命令自动选择第一块非回环活动网卡，因此不要求你的连接名一定叫 `ens33`。

### 4.2 控制平面 `nl-cp1`

```bash
CON_NAME="$(nmcli -t -f NAME,DEVICE connection show --active | awk -F: '$2 != "lo" {print $1; exit}')"
sudo hostnamectl set-hostname nl-cp1
sudo nmcli connection modify "$CON_NAME" \
  ipv4.method manual \
  ipv4.addresses 192.168.66.110/24 \
  ipv4.gateway 192.168.66.2 \
  ipv4.dns "192.168.66.2 1.1.1.1" \
  ipv4.ignore-auto-dns yes
sudo nmcli connection up "$CON_NAME"
```

### 4.3 工作节点 `nl-worker1`

```bash
CON_NAME="$(nmcli -t -f NAME,DEVICE connection show --active | awk -F: '$2 != "lo" {print $1; exit}')"
sudo hostnamectl set-hostname nl-worker1
sudo nmcli connection modify "$CON_NAME" \
  ipv4.method manual \
  ipv4.addresses 192.168.66.111/24 \
  ipv4.gateway 192.168.66.2 \
  ipv4.dns "192.168.66.2 1.1.1.1" \
  ipv4.ignore-auto-dns yes
sudo nmcli connection up "$CON_NAME"
```

### 4.4 工作节点 `nl-worker2`

```bash
CON_NAME="$(nmcli -t -f NAME,DEVICE connection show --active | awk -F: '$2 != "lo" {print $1; exit}')"
sudo hostnamectl set-hostname nl-worker2
sudo nmcli connection modify "$CON_NAME" \
  ipv4.method manual \
  ipv4.addresses 192.168.66.112/24 \
  ipv4.gateway 192.168.66.2 \
  ipv4.dns "192.168.66.2 1.1.1.1" \
  ipv4.ignore-auto-dns yes
sudo nmcli connection up "$CON_NAME"
```

### 4.5 每台机器验证网络

```bash
hostnamectl --static
ip -4 address
ip route
ping -c 3 192.168.66.2
ping -c 3 192.168.66.1
curl -I --connect-timeout 10 https://pkgs.k8s.io/
```

必须确认：

- 主机名与节点规划一致；
- 默认路由指向 `192.168.66.2`；
- 能访问宿主机 VMnet8 地址；
- 能访问软件仓库。

## 5. 配置 FinalShell 连接

### 5.1 三台节点安装并启动 SSH

每台机器在 VMware 控制台执行：

```bash
sudo dnf install -y openssh-server openssh-clients
sudo systemctl enable --now sshd
sudo systemctl status sshd --no-pager
sudo ss -lntp | grep ':22'
```

预期结果：

- `sshd` 状态为 `active (running)`；
- `ss` 输出包含 `0.0.0.0:22` 或 `[::]:22`。

初次连接阶段保留普通用户密码登录，但禁止直接使用 root：

```bash
sudo install -d -m 0755 /etc/ssh/sshd_config.d
sudo tee /etc/ssh/sshd_config.d/10-northledger-lab.conf >/dev/null <<'EOF'
PermitRootLogin no
PubkeyAuthentication yes
PasswordAuthentication yes
ClientAliveInterval 60
ClientAliveCountMax 3
EOF
sudo sshd -t
sudo systemctl reload sshd
```

`sshd -t` 没有输出表示语法检查通过；若有错误，不要 reload，先根据错误行修正。

### 5.2 Windows 侧先测试 22 端口

在 Windows PowerShell 执行：

```powershell
Test-NetConnection 192.168.66.110 -Port 22
Test-NetConnection 192.168.66.111 -Port 22
Test-NetConnection 192.168.66.112 -Port 22
```

每个结果都应显示：

```text
TcpTestSucceeded : True
```

也可以使用系统自带 SSH 做交叉验证：

```powershell
ssh opsadmin@192.168.66.110
ssh opsadmin@192.168.66.111
ssh opsadmin@192.168.66.112
```

首次出现主机指纹提示时，先核对 IP，再输入 `yes`。密码输入时不会显示字符，这是正常现象。

### 5.3 FinalShell 新建三个连接

在 FinalShell 中依次新建 SSH 连接：

| 连接名称 | 主机 | 端口 | 用户名 |
| --- | --- | ---: | --- |
| `NorthLedger-CP1` | `192.168.66.110` | 22 | `opsadmin` |
| `NorthLedger-Worker1` | `192.168.66.111` | 22 | `opsadmin` |
| `NorthLedger-Worker2` | `192.168.66.112` | 22 | `opsadmin` |

认证方式先选择密码。不要勾选 root 登录，不要把密码导出到公开配置包。

建议配置：

- 字符编码：UTF-8；
- SSH keepalive：60 秒；
- 连接超时：10-20 秒；
- 初始目录：`/home/opsadmin`。

登录每台机器后执行：

```bash
whoami
hostnamectl --static
ip -4 route get 192.168.66.1
sudo -v
```

结果应分别证明：登录用户是 `opsadmin`、主机名正确、到 Windows 宿主机有路由，并且账号拥有 sudo 权限。

### 5.4 FinalShell 文件上传目录

每台机器执行：

```bash
mkdir -p /home/opsadmin/northledger-upload
chmod 700 /home/opsadmin/northledger-upload
```

以后通过 FinalShell 文件管理器把项目脚本上传到该目录，不直接上传到 `/root`、`/etc` 或 `/usr/local/bin`。上传后先检查，再使用 sudo 安装：

```bash
ls -lah /home/opsadmin/northledger-upload
sha256sum /home/opsadmin/northledger-upload/文件名
```

## 6. 所有节点共同的系统准备

以下命令需要在 `nl-cp1`、`nl-worker1`、`nl-worker2` 分别执行。

### 6.1 更新系统与安装基础工具

先验证默认仓库。若 `dnf makecache` 成功且没有 checksum mismatch 或超时，可直接执行安装；若出现 `mirror.ps.kz` 超时、`repomd.xml` 校验不一致或 `All mirrors were tried`，执行 6.1.1 的固定镜像修复。

```bash
sudo dnf clean all
sudo dnf makecache --refresh
```

成功后安装基础工具：

```bash
sudo dnf install -y \
  curl wget vim git jq tar socat conntrack-tools \
  iproute iptables ebtables ethtool chrony dnf-plugins-core
sudo systemctl enable --now chronyd
timedatectl
chronyc tracking
```

系统大版本更新不要和 Kubernetes 安装混在同一批操作里。确需更新时先创建虚拟机快照，再单独执行 `sudo dnf -y update` 并重启验证。

### 6.1.1 软件源 checksum mismatch 或超时的恢复流程

以下流程是本次三节点实操中真实遇到的恢复方案。它关闭会随机选择远端站点的 BaseOS/AppStream/CRB 定义，改为固定国内同步站，同时保留 RPM GPG 签名校验。

先确认目标仓库可达：

```bash
curl -I --connect-timeout 10 \
  https://mirrors.aliyun.com/centos-stream/9-stream/BaseOS/x86_64/os/repodata/repomd.xml
```

必须看到 HTTP 200。随后备份仓库配置并创建固定源：

```bash
sudo test -e /etc/yum.repos.d.backup-before-kubernetes || \
  sudo cp -a /etc/yum.repos.d /etc/yum.repos.d.backup-before-kubernetes
sudo dnf config-manager --set-disabled baseos
sudo dnf config-manager --set-disabled appstream
sudo dnf config-manager --set-disabled crb

sudo tee /etc/yum.repos.d/centos-stream-direct.repo >/dev/null <<'EOF'
[baseos-direct]
name=CentOS Stream 9 - BaseOS Direct
baseurl=https://mirrors.aliyun.com/centos-stream/9-stream/BaseOS/$basearch/os/
enabled=1
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-centosofficial
metadata_expire=1h

[appstream-direct]
name=CentOS Stream 9 - AppStream Direct
baseurl=https://mirrors.aliyun.com/centos-stream/9-stream/AppStream/$basearch/os/
enabled=1
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-centosofficial
metadata_expire=1h

[crb-direct]
name=CentOS Stream 9 - CRB Direct
baseurl=https://mirrors.aliyun.com/centos-stream/9-stream/CRB/$basearch/os/
enabled=1
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-centosofficial
metadata_expire=1h
EOF

sudo dnf clean all
sudo dnf \
  --disablerepo='*' \
  --enablerepo=baseos-direct \
  --enablerepo=appstream-direct \
  --enablerepo=crb-direct \
  makecache --refresh
```

最后一条必须显示 `Metadata cache created`。若仍失败，先排查 DNS、默认路由和宿主机网络，不要通过关闭 GPG 校验绕过问题。

### 6.2 写入三节点 hosts

```bash
sudo tee -a /etc/hosts >/dev/null <<'EOF'
192.168.66.110 nl-cp1
192.168.66.111 nl-worker1
192.168.66.112 nl-worker2
EOF

getent hosts nl-cp1 nl-worker1 nl-worker2
```

如果 `/etc/hosts` 已有这些名字，先检查并删除旧行，不能让一个主机名对应两个地址。

### 6.3 禁用 swap

```bash
sudo swapoff -a
sudo cp -a /etc/fstab /etc/fstab.before-kubernetes
sudo sed -ri '/\sswap\s/s/^/# kubernetes-disabled: /' /etc/fstab
swapon --show
free -h
```

`swapon --show` 应无输出。备份文件用于发生错误时恢复，不要重复执行覆盖备份。

### 6.4 SELinux 调整

```bash
sudo setenforce 0
sudo sed -ri 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
getenforce
```

预期为 `Permissive`。这是 kubeadm 官方当前对 Red Hat 系发行版给出的兼容做法，会降低强制访问控制强度；本项目必须如实记录，不能把它描述为已经完成 SELinux 强化。

### 6.5 内核模块与转发参数

```bash
sudo tee /etc/modules-load.d/kubernetes.conf >/dev/null <<'EOF'
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

sudo tee /etc/sysctl.d/99-kubernetes-cri.conf >/dev/null <<'EOF'
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system
lsmod | grep -E 'overlay|br_netfilter'
sysctl net.ipv4.ip_forward
```

预期 `net.ipv4.ip_forward = 1`。

### 6.6 避免 NetworkManager 接管 Calico 接口

```bash
sudo tee /etc/NetworkManager/conf.d/calico.conf >/dev/null <<'EOF'
[keyfile]
unmanaged-devices=interface-name:cali*;interface-name:tunl*;interface-name:vxlan.calico
EOF
sudo systemctl reload NetworkManager
```

### 6.7 处理 firewalld

Calico 官方说明 firewalld 等 iptables 管理器可能干扰 Calico 写入的规则，因此本实验集群禁用 firewalld，由 Kubernetes NetworkPolicy/Calico 策略承担 Pod 网络控制：

```bash
sudo systemctl disable --now firewalld
systemctl is-enabled firewalld || true
systemctl is-active firewalld || true
```

这不等于“生产环境永远应该关闭主机防火墙”。如果未来需要主机层策略，应单独设计 Calico HostEndpoint/GlobalNetworkPolicy，并验证不会切断 SSH 和 Kubernetes 控制面。

## 7. 所有节点安装 containerd

只安装 Kubernetes 所需的 `containerd.io`，不在节点上运行 Docker Engine：

```bash
sudo test -f /etc/yum.repos.d/docker-ce.repo || \
  sudo dnf config-manager --add-repo \
  https://download.docker.com/linux/centos/docker-ce.repo

sudo dnf install -y containerd.io
```

先确认软件确实安装成功。`sudo install -d` 只负责创建目录，不等于安装 containerd：

```bash
rpm -q containerd.io
command -v containerd
containerd --version
```

然后生成配置、启用 CRI 与 systemd cgroup：

```bash
sudo install -d -m 0755 /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -ri 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml

# Docker 仓库提供的 containerd 默认配置可能显式禁用 CRI；Kubernetes 必须启用它。
sudo sed -ri \
  '/^[[:space:]]*disabled_plugins[[:space:]]*=/ s/^/#/' \
  /etc/containerd/config.toml

sudo systemctl enable --now containerd
sudo systemctl restart containerd
```

检查 CRI 和 cgroup：

```bash
printf 'containerd status: '
sudo systemctl is-active containerd
containerd --version
sudo grep -n 'SystemdCgroup' /etc/containerd/config.toml
sudo grep -n 'disabled_plugins' /etc/containerd/config.toml || true
sudo ctr plugins ls | grep -E 'io.containerd.grpc.v1.cri|io.containerd.cri.v1' \
  || echo 'CRI NOT READY'
```

要求：

- `SystemdCgroup = true`；
- `disabled_plugins` 行已被注释，或者列表中不包含 `cri`；
- containerd 为 `active (running)`；
- CRI 插件状态为 `ok`。

## 8. 所有节点安装 Kubernetes 1.36.4

### 8.1 配置 Kubernetes 1.36 软件仓库

```bash
sudo tee /etc/yum.repos.d/kubernetes.repo >/dev/null <<'EOF'
[kubernetes]
name=Kubernetes
baseurl=https://pkgs.k8s.io/core:/stable:/v1.36/rpm/
enabled=1
gpgcheck=1
gpgkey=https://pkgs.k8s.io/core:/stable:/v1.36/rpm/repodata/repomd.xml.key
exclude=kubelet kubeadm kubectl cri-tools kubernetes-cni
EOF

sudo dnf makecache --refresh

sudo dnf install -y \
  kubelet-1.36.4 \
  kubeadm-1.36.4 \
  kubectl-1.36.4 \
  --disableexcludes=kubernetes
sudo systemctl enable --now kubelet
```

检查版本：

```bash
kubeadm version -o short
kubelet --version
kubectl version --client=true
```

此时 kubelet 可能反复重启，因为集群尚未初始化，这是 kubeadm 安装阶段的正常状态。使用下面命令确认原因，而不是盲目重装：

```bash
sudo systemctl status kubelet --no-pager
sudo journalctl -u kubelet -n 50 --no-pager
```

## 9. 三节点初始化前联合检查

### 9.1 每台节点检查唯一性

```bash
hostnamectl --static
cat /sys/class/dmi/id/product_uuid
ip link show | grep -E 'link/ether'
```

三个节点的主机名、`product_uuid` 和主网卡 MAC 必须不同。

### 9.2 节点互通检查

在 `nl-cp1` 执行：

```bash
ping -c 3 nl-worker1
ping -c 3 nl-worker2
nc -vz nl-worker1 22
nc -vz nl-worker2 22
```

在两个 worker 分别执行：

```bash
ping -c 3 nl-cp1
nc -vz nl-cp1 22
```

## 10. 仅在控制平面 `nl-cp1` 执行

### 10.1 拉取控制面镜像

```bash
sudo kubeadm config images pull \
  --kubernetes-version v1.36.4 \
  --cri-socket unix:///run/containerd/containerd.sock
```

### 10.2 初始化控制平面

```bash
sudo kubeadm init \
  --kubernetes-version v1.36.4 \
  --apiserver-advertise-address 192.168.66.110 \
  --control-plane-endpoint 192.168.66.110:6443 \
  --pod-network-cidr 10.244.0.0/16 \
  --service-cidr 10.96.0.0/12 \
  --cri-socket unix:///run/containerd/containerd.sock
```

成功后立即保存输出中的 `kubeadm join ...` 命令。令牌是临时凭据，不要提交到 GitHub。

### 10.3 配置普通用户 kubectl

```bash
mkdir -p "$HOME/.kube"
sudo cp -i /etc/kubernetes/admin.conf "$HOME/.kube/config"
sudo chown "$(id -u):$(id -g)" "$HOME/.kube/config"
chmod 600 "$HOME/.kube/config"

kubectl get nodes -o wide
```

安装 CNI 前，控制平面显示 `NotReady` 是正常现象。

### 10.4 安装 Calico 3.32.2

```bash
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.32.2/manifests/v1_crd_projectcalico_org.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.32.2/manifests/tigera-operator.yaml
```

创建与 VMware 网段不冲突的 Calico 安装资源：

```bash
cat > "$HOME/calico-installation.yaml" <<'EOF'
apiVersion: operator.tigera.io/v1
kind: Installation
metadata:
  name: default
spec:
  calicoNetwork:
    ipPools:
      - name: default-ipv4-ippool
        blockSize: 26
        cidr: 10.244.0.0/16
        encapsulation: VXLAN
        natOutgoing: Enabled
        nodeSelector: all()
EOF

kubectl create -f "$HOME/calico-installation.yaml"
kubectl get tigerastatus
kubectl get pods -A -o wide
```

等待 Calico 可用：

```bash
watch kubectl get tigerastatus
```

按 `Ctrl+C` 退出 watch。不要把“命令一直不返回”误认为卡死。

## 11. 仅在两个工作节点执行 join

把 `kubeadm init` 输出中的 join 命令分别粘贴到 `nl-worker1` 和 `nl-worker2`。形式类似：

```bash
sudo kubeadm join 192.168.66.110:6443 \
  --token 实际令牌 \
  --discovery-token-ca-cert-hash sha256:实际哈希 \
  --cri-socket unix:///run/containerd/containerd.sock
```

如果令牌过期，在 `nl-cp1` 重新生成：

```bash
kubeadm token create --print-join-command
```

然后在输出命令末尾补上：

```text
--cri-socket unix:///run/containerd/containerd.sock
```

## 12. 回到 `nl-cp1` 完成节点标签

```bash
kubectl get nodes -o wide
kubectl label node nl-worker1 node-role.kubernetes.io/worker=worker
kubectl label node nl-worker2 node-role.kubernetes.io/worker=worker
kubectl label node nl-worker1 northledger.io/workload=true
kubectl label node nl-worker2 northledger.io/workload=true
kubectl get nodes --show-labels
```

控制平面保留默认污点，不删除：

```bash
kubectl describe node nl-cp1 | grep -i Taints
```

应看到 `node-role.kubernetes.io/control-plane:NoSchedule`。

## 13. 集群网络与 DNS 验收

在 `nl-cp1` 执行：

```bash
kubectl get nodes -o wide
kubectl get pods -A -o wide
kubectl get svc -A
kubectl cluster-info
```

创建临时测试工作负载：

```bash
kubectl create namespace cluster-test
kubectl create deployment web-test \
  --namespace cluster-test \
  --image nginx:1.29-alpine \
  --replicas 2
kubectl expose deployment web-test \
  --namespace cluster-test \
  --port 80
kubectl rollout status deployment/web-test --namespace cluster-test --timeout=180s
kubectl get pods --namespace cluster-test -o wide
```

验证 Service DNS 与 HTTP：

```bash
kubectl run dns-test \
  --namespace cluster-test \
  --rm -it \
  --restart=Never \
  --image busybox:1.37.0 \
  -- nslookup web-test.cluster-test.svc.cluster.local

kubectl run http-test \
  --namespace cluster-test \
  --rm -it \
  --restart=Never \
  --image busybox:1.37.0 \
  -- wget -qO- http://web-test
```

完成后清理测试命名空间：

```bash
kubectl delete namespace cluster-test
```

## 14. 为有状态组件准备节点目录

仅在 `nl-worker1`：

```bash
sudo install -d -m 0750 /data/northledger/mysql
sudo install -d -m 0750 /data/northledger/prometheus
sudo ls -ld /data/northledger/mysql /data/northledger/prometheus
```

仅在 `nl-worker2`：

```bash
sudo install -d -m 0750 /data/northledger/redis
sudo install -d -m 0750 /data/northledger/loki
sudo ls -ld /data/northledger/redis /data/northledger/loki
```

现在只创建目录，不提前把目录权限改成 `777`。实际 UID/GID 将在 Helm 有状态工作负载确定后设置。

## 15. FinalShell 连接故障排查

按以下顺序检查，不要直接重装 SSH。

### 15.1 Windows 能否到达虚拟机

```powershell
ping 192.168.66.110
Test-NetConnection 192.168.66.110 -Port 22
```

### 15.2 虚拟机地址和路由是否正确

在 VMware 控制台执行：

```bash
ip -4 address
ip route
ping -c 3 192.168.66.1
```

### 15.3 SSH 服务是否正常

```bash
sudo systemctl status sshd --no-pager
sudo ss -lntp | grep ':22'
sudo sshd -t
sudo journalctl -u sshd -n 100 --no-pager
```

### 15.4 账号是否正常

```bash
id opsadmin
sudo passwd -S opsadmin
sudo faillock --user opsadmin
```

### 15.5 FinalShell 报主机指纹变化

只有在你确认该 IP 的虚拟机确实被重装或重建后，才删除 Windows 旧指纹：

```powershell
ssh-keygen -R 192.168.66.110
```

随后重新连接并核对新指纹。不要在不知道原因时直接忽略指纹变化。

## 16. Kubernetes 常见故障排查

### 节点 `NotReady`

```bash
kubectl describe node 节点名
kubectl get pods -A -o wide
sudo journalctl -u kubelet -n 200 --no-pager
sudo crictl --runtime-endpoint unix:///run/containerd/containerd.sock ps -a
```

### Pod 一直 `Pending`

```bash
kubectl describe pod Pod名称 -n 命名空间
kubectl get events -A --sort-by=.lastTimestamp | tail -n 50
kubectl top nodes 2>/dev/null || true
```

### 镜像拉取失败

```bash
kubectl describe pod Pod名称 -n 命名空间
sudo crictl --runtime-endpoint unix:///run/containerd/containerd.sock pull 镜像名
curl -I --connect-timeout 10 https://registry.k8s.io/v2/
```

### 跨节点网络失败

```bash
ip link show vxlan.calico
sudo ss -lunp | grep 4789
kubectl get pods -n calico-system -o wide
kubectl logs -n calico-system -l k8s-app=calico-node --tail=100
```

## 17. 第一阶段完成标准

只有全部满足才算集群搭建完成：

- 三台节点均为 `Ready`；
- 控制平面保留 `NoSchedule` 污点；
- 两个工作节点都有业务标签；
- CoreDNS、Calico 组件正常；
- 临时双副本 Nginx 能在 worker 上运行；
- Pod 能解析 Service DNS 并完成 HTTP 请求；
- 三台机器能从 FinalShell 独立连接；
- containerd 和 kubelet 均设置为开机启动；
- 没有把密码、token、kubeconfig 提交到 GitHub；
- MySQL、Redis、Prometheus、Loki 目录已按节点分开，但尚未宣称高可用。

## 18. 集群完成后的项目部署边界

截至本手册修订时，仓库中的 Helm Chart 能部署：

- React/Nginx Web Deployment 两副本；
- Spring Boot API Deployment 两副本并由 HPA 管理；
- MySQL 单副本 StatefulSet 与 PVC；
- Redis 单副本 StatefulSet、AOF 与 Redis Exporter；
- ConfigMap、Secret、ServiceAccount、PDB、NetworkPolicy、Ingress 和 Helm test。

这套资源可以作为三节点 Kubernetes 的真实应用交付与运维对象，但当前后端仍是模块化单体，不能在简历或面试中称为“已经完成微服务拆分”。最终目标会继续拆分身份、账务、对账报表等独立服务；拆分完成后仍复用本手册的集群、镜像、存储、Helm、监控和故障处理流程。

## 19. 部署前资源门禁

截图中 `nl-worker2` 只显示约 1.6 GiB 可用内存，通常意味着虚拟机只分配了 2 GiB。它足以加入集群，但不足以同时稳定承载业务、MySQL、Redis、Prometheus、Grafana 和 Loki。

推荐关机后在 VMware 调整为：

| 节点 | vCPU | 内存 | 主要职责 |
| --- | ---: | ---: | --- |
| `nl-cp1` | 2 | 2.5～3 GiB | API Server、etcd、调度与管理 |
| `nl-worker1` | 2 | 4～4.5 GiB | API/Web、MySQL、Prometheus |
| `nl-worker2` | 2 | 4～4.5 GiB | API/Web、Redis、Grafana/Loki |

在 `nl-cp1` 检查容量：

```bash
kubectl get nodes
kubectl get nodes \
  -o custom-columns='NAME:.metadata.name,CPU:.status.capacity.cpu,MEMORY:.status.capacity.memory'
kubectl top nodes 2>/dev/null || true
```

若总内存达不到约 10 GiB，先部署应用与数据库，监控栈延后；不要通过删除资源限制来掩盖容量不足。

## 20. 三节点标签和本地持久化存储

### 20.1 在 `nl-cp1` 设置调度标签

```bash
kubectl label node nl-worker1 northledger.io/workload=true --overwrite
kubectl label node nl-worker2 northledger.io/workload=true --overwrite
kubectl label node nl-worker1 northledger.io/storage=mysql --overwrite
kubectl label node nl-worker2 northledger.io/storage=redis --overwrite
kubectl get nodes -L northledger.io/workload,northledger.io/storage
```

标签表达调度意图。MySQL/Redis 的真正固定关系由后面的 PV `nodeAffinity` 实现，不能只依赖口头约定。

### 20.2 仅在 `nl-worker1` 准备 MySQL 目录

```bash
sudo install -d -m 0750 -o 999 -g 999 /data/northledger/mysql
sudo ls -ld /data/northledger/mysql
df -h /data/northledger/mysql
```

### 20.3 仅在 `nl-worker2` 准备 Redis 目录

```bash
sudo install -d -m 0750 -o 999 -g 1000 /data/northledger/redis
sudo ls -ld /data/northledger/redis
df -h /data/northledger/redis
```

### 20.4 在 `nl-cp1` 创建静态 Local PV

```bash
mkdir -p /home/opsadmin/northledger-deploy

cat >/home/opsadmin/northledger-deploy/local-storage.yaml <<'EOF'
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: northledger-mysql-local
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Retain
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: northledger-redis-local
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Retain
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: northledger-mysql-pv
spec:
  capacity:
    storage: 5Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: northledger-mysql-local
  local:
    path: /data/northledger/mysql
  nodeAffinity:
    required:
      nodeSelectorTerms:
        - matchExpressions:
            - key: kubernetes.io/hostname
              operator: In
              values:
                - nl-worker1
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: northledger-redis-pv
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: northledger-redis-local
  local:
    path: /data/northledger/redis
  nodeAffinity:
    required:
      nodeSelectorTerms:
        - matchExpressions:
            - key: kubernetes.io/hostname
              operator: In
              values:
                - nl-worker2
EOF

kubectl apply -f /home/opsadmin/northledger-deploy/local-storage.yaml
kubectl get storageclass
kubectl get pv
```

预期两个 PV 为 `Available`。Local PV 能证明持久化、节点亲和性、备份恢复和故障边界，但节点磁盘损坏时不能自动迁移，因此不属于数据库高可用。

## 21. Windows 构建和验证应用镜像

以下命令在 Windows PowerShell 的 `D:\Develop\OpsPilot` 中执行，不在虚拟机执行。

### 21.1 先执行质量门禁

```powershell
Set-Location D:\Develop\OpsPilot

mvn -B -ntp test
npm --prefix frontend ci --ignore-scripts
npm --prefix frontend audit --audit-level=moderate
npm --prefix frontend run build
```

任何命令失败都停止。不能使用 `-DskipTests` 生成候选发布包。

### 21.2 构建固定版本镜像

```powershell
$ReleaseVersion = '0.3.0'

docker build `
  --build-arg APP_VERSION=$ReleaseVersion `
  -t opspilot-api:$ReleaseVersion .

docker build `
  -f frontend/Dockerfile `
  -t opspilot-web:$ReleaseVersion .

docker pull mysql:8.4
docker pull redis:7.4.10-alpine
docker pull oliver006/redis_exporter:v1.89.0-alpine
```

### 21.3 导出镜像归档并生成校验值

```powershell
New-Item -ItemType Directory -Force D:\Develop\OpsPilot\dist\k8s-three-node | Out-Null

docker save `
  --output D:\Develop\OpsPilot\dist\k8s-three-node\northledger-images-0.3.0.tar `
  opspilot-api:0.3.0 `
  opspilot-web:0.3.0 `
  mysql:8.4 `
  redis:7.4.10-alpine `
  oliver006/redis_exporter:v1.89.0-alpine

Get-FileHash `
  D:\Develop\OpsPilot\dist\k8s-three-node\northledger-images-0.3.0.tar `
  -Algorithm SHA256
```

保存 SHA-256。镜像标签与 Helm values 必须一致，不能依赖含义不确定的 `latest`。

## 22. 使用 FinalShell 向两个 worker 分发镜像

使用 FinalShell 文件面板把下列文件分别上传到 `nl-worker1` 和 `nl-worker2`：

```text
D:\Develop\OpsPilot\dist\k8s-three-node\northledger-images-0.3.0.tar
```

目标目录：

```text
/home/opsadmin/northledger-upload/
```

分别在两个 worker 执行：

```bash
cd /home/opsadmin/northledger-upload
sha256sum northledger-images-0.3.0.tar
sudo ctr -n k8s.io images import northledger-images-0.3.0.tar
sudo ctr -n k8s.io images list | grep -E 'opspilot|mysql|redis'
```

两个节点上的归档 SHA-256 必须和 Windows 一致。API/Web 需要导入两个 worker，因为 Deployment 副本可能调度到任意工作节点。

## 23. 在 `nl-cp1` 安装 Helm 并上传 Chart

### 23.1 安装与 CI 一致的 Helm 3.18.6

```bash
cd /tmp
curl -fLO https://get.helm.sh/helm-v3.18.6-linux-amd64.tar.gz
curl -fLO https://get.helm.sh/helm-v3.18.6-linux-amd64.tar.gz.sha256sum
sha256sum -c helm-v3.18.6-linux-amd64.tar.gz.sha256sum
tar -xzf helm-v3.18.6-linux-amd64.tar.gz
sudo install -m 0755 linux-amd64/helm /usr/local/bin/helm
helm version
```

### 23.2 使用 FinalShell 上传 Helm Chart

把 Windows 目录：

```text
D:\Develop\OpsPilot\deploy\k8s\helm\opspilot
```

上传为：

```text
/home/opsadmin/northledger-deploy/opspilot
```

然后在 `nl-cp1` 验证：

```bash
helm lint /home/opsadmin/northledger-deploy/opspilot \
  --set-string secrets.databasePassword=lint-only-database-password \
  --set-string secrets.mysqlRootPassword=lint-only-root-password \
  --set-string secrets.redisPassword=lint-only-redis-password \
  --set-string secrets.bootstrapAdminPassword=lint-only-admin-password
```

`lint-only-*` 只用于本地模板校验，不用于实际部署。

## 24. 创建运行 Secret 与三节点覆盖值

### 24.1 创建命名空间和 Secret

在 `nl-cp1` 执行。密码通过隐藏输入进入当前 Shell 内存，不写入 values 文件：

```bash
kubectl create namespace opspilot --dry-run=client -o yaml | kubectl apply -f -

read -rsp 'Database password: ' NORTHLEDGER_DB_PASSWORD; echo
read -rsp 'MySQL root password: ' NORTHLEDGER_ROOT_PASSWORD; echo
read -rsp 'Redis password: ' NORTHLEDGER_REDIS_PASSWORD; echo
read -rsp 'Bootstrap admin password: ' NORTHLEDGER_ADMIN_PASSWORD; echo

kubectl -n opspilot create secret generic opspilot-runtime \
  --from-literal=database-password="${NORTHLEDGER_DB_PASSWORD}" \
  --from-literal=mysql-root-password="${NORTHLEDGER_ROOT_PASSWORD}" \
  --from-literal=redis-password="${NORTHLEDGER_REDIS_PASSWORD}" \
  --from-literal=bootstrap-admin-username='admin' \
  --from-literal=bootstrap-admin-password="${NORTHLEDGER_ADMIN_PASSWORD}" \
  --from-literal=bootstrap-admin-display-name='Kubernetes Administrator' \
  --dry-run=client -o yaml | kubectl apply -f -

unset NORTHLEDGER_DB_PASSWORD \
  NORTHLEDGER_ROOT_PASSWORD \
  NORTHLEDGER_REDIS_PASSWORD \
  NORTHLEDGER_ADMIN_PASSWORD

kubectl -n opspilot get secret opspilot-runtime
```

只检查 Secret 存在，不执行会输出明文的解码命令。

### 24.2 创建三节点覆盖值

```bash
cat >/home/opsadmin/northledger-deploy/values-three-node.yaml <<'EOF'
global:
  environment: kubernetes-three-node
  version: "0.3.0"

api:
  replicaCount: 2
  image:
    repository: opspilot-api
    tag: "0.3.0"
    pullPolicy: IfNotPresent
  sessionCookieSecure: false
  autoscaling:
    enabled: false

web:
  replicaCount: 2
  image:
    repository: opspilot-web
    tag: "0.3.0"
    pullPolicy: IfNotPresent

mysql:
  enabled: true
  persistence:
    size: 5Gi
    storageClass: northledger-mysql-local

database:
  host: mysql
  port: 3306
  name: opspilot
  username: opspilot

redis:
  enabled: true
  persistence:
    size: 1Gi
    storageClass: northledger-redis-local

secrets:
  create: false
  existingSecret: opspilot-runtime

ingress:
  enabled: false
EOF
```

首次部署关闭 HPA 和 Ingress，是为了先验证基础工作负载、存储和服务调用。Metrics Server 与 Ingress Controller 验收后再开启对应能力，不能在依赖缺失时声称 HPA/Ingress 已可用。

## 25. Helm 安装、检查与业务验收

### 25.1 首次安装

```bash
helm upgrade --install opspilot \
  /home/opsadmin/northledger-deploy/opspilot \
  --namespace opspilot \
  --values /home/opsadmin/northledger-deploy/values-three-node.yaml \
  --atomic \
  --timeout 10m
```

### 25.2 检查资源

```bash
helm list -n opspilot
helm status opspilot -n opspilot
kubectl -n opspilot get pods -o wide
kubectl -n opspilot get svc
kubectl -n opspilot get pvc
kubectl get pv
kubectl -n opspilot get pdb,networkpolicy
kubectl -n opspilot get events --sort-by=.lastTimestamp | tail -n 50
```

预期：

- `mysql-0` 位于 `nl-worker1`，MySQL PVC 为 `Bound`；
- `opspilot-redis-0` 位于 `nl-worker2`，Redis PVC 为 `Bound`；
- API 与 Web 各两个 Pod，尽量分布到两个 worker；
- 所有容器 `READY` 完整且重启次数不持续增加。

### 25.3 Helm Release Test

```bash
helm test opspilot -n opspilot --logs
```

它从集群内部访问 Web `/health`，并穿过 Nginx、API readiness、MySQL 和 Redis 依赖链。成功只能证明基础依赖链可用，还不能替代真实转账业务验收。

### 25.4 临时端口转发供 Windows 浏览器访问

在 `nl-cp1` 保持以下命令运行：

```bash
kubectl -n opspilot port-forward \
  --address 0.0.0.0 \
  service/opspilot-web \
  18000:8080
```

Windows 浏览器打开：

```text
http://192.168.66.110:18000
```

端口转发只用于首次验收，FinalShell 断开后会停止，不属于长期入口。

### 25.5 Windows 执行业务冒烟

先把控制平面 kubeconfig 安全复制到 Windows，或在能够访问集群的 PowerShell 中配置 `kubectl`。随后执行仓库脚本：

```powershell
Set-Location D:\Develop\OpsPilot
powershell -ExecutionPolicy Bypass -File scripts\k8s\smoke-test.ps1
```

脚本会验证登录、CSRF、账户创建、真实转账、幂等重放、双边流水、Redis 会话、缓存 TTL 和事务提交后的缓存失效。只有脚本返回通过，才算形成应用级闭环。

## 26. 发布、回滚与故障演练

### 26.1 发布新版本

构建并向两个 worker 导入新标签后，修改覆盖文件中的 API/Web 标签，再执行：

```bash
helm upgrade opspilot \
  /home/opsadmin/northledger-deploy/opspilot \
  --namespace opspilot \
  --values /home/opsadmin/northledger-deploy/values-three-node.yaml \
  --atomic \
  --timeout 10m

kubectl -n opspilot rollout status deployment/opspilot-api --timeout=5m
kubectl -n opspilot rollout status deployment/opspilot-web --timeout=5m
helm history opspilot -n opspilot
```

### 26.2 Helm 回滚

```bash
helm history opspilot -n opspilot
helm rollback opspilot 上一个成功REVISION -n opspilot --wait --timeout 10m
kubectl -n opspilot get pods -o wide
helm test opspilot -n opspilot --logs
```

回滚后必须重新执行业务冒烟，不能只观察 `rollout status`。

### 26.3 Pod 自愈

```bash
kubectl -n opspilot get pods -l app.kubernetes.io/component=api -o wide
kubectl -n opspilot delete pod 一个API_POD名称
kubectl -n opspilot get pods -l app.kubernetes.io/component=api -o wide --watch
```

记录旧 Pod、替代 Pod、所在节点、恢复时间和期间业务请求结果。

### 26.4 Redis 故障降级

```bash
kubectl -n opspilot scale statefulset opspilot-redis --replicas=0
kubectl -n opspilot logs deployment/opspilot-api --tail=200
kubectl -n opspilot get pods
```

观察 readiness、登录会话、业务缓存降级日志和指标。实验结束后恢复：

```bash
kubectl -n opspilot scale statefulset opspilot-redis --replicas=1
kubectl -n opspilot rollout status statefulset/opspilot-redis --timeout=5m
```

### 26.5 MySQL 备份与恢复

备份和恢复必须按仓库中的 `docs/runbooks/mysql-backup-restore.md` 执行。备份文件要复制到节点目录之外，并记录：备份时间、业务数据范围、SHA-256、恢复耗时和复测结果。

## 27. 统一排障路径

遇到页面打不开、Pod 异常或业务错误时，按下面顺序收集证据：

```text
DNS/IP
  → 路由、端口、firewalld
  → Node Ready、CNI、CoreDNS
  → Pod 状态和 Events
  → Deployment/StatefulSet/Service/EndpointSlice
  → readiness/liveness/startup 探针
  → 应用日志与 traceId
  → MySQL/Redis
  → 止损或回滚
  → 业务复测
  → 复盘记录
```

常用命令：

```bash
kubectl get nodes -o wide
kubectl get pods -A -o wide
kubectl -n opspilot get events --sort-by=.lastTimestamp | tail -n 80
kubectl -n opspilot describe pod POD名称
kubectl -n opspilot logs POD名称 -c 容器名称 --tail=200
kubectl -n opspilot get svc,endpointslice
kubectl -n opspilot get pvc
helm status opspilot -n opspilot
helm history opspilot -n opspilot
```

## 28. 最终验收清单

### 28.1 集群层

- [ ] 三个节点均为 `Ready`；
- [ ] containerd 为 `active`，CRI 插件为 `ok`；
- [ ] Calico 和 CoreDNS 正常；
- [ ] 跨节点 Pod 网络和 Service DNS 正常；
- [ ] 控制平面保持污点，业务运行在 worker；
- [ ] 节点重启后 containerd、kubelet 和网络自动恢复。

### 28.2 交付层

- [ ] 镜像带不可变版本标签，两个 worker 的镜像一致；
- [ ] Helm lint、安装、Release Test 和历史记录可复现；
- [ ] API/Web 多副本分布在两个 worker；
- [ ] Secret 不进入 Git、values 和终端输出；
- [ ] 更新失败能由 `--atomic` 自动回滚；
- [ ] 人工回滚后完成业务复测。

### 28.3 数据层

- [ ] MySQL/Redis PVC 均为 `Bound`；
- [ ] 删除并重建 Pod 后数据仍存在；
- [ ] MySQL/Redis 所在节点和 Local PV 亲和性一致；
- [ ] 备份能在空库完成恢复并通过业务校验；
- [ ] 文档明确单副本不是高可用。

### 28.4 业务层

- [ ] 管理员登录与服务端会话正常；
- [ ] 能创建账户并完成转账；
- [ ] 相同幂等键重放不会重复扣款；
- [ ] 双边流水数量和金额平衡；
- [ ] Redis 缓存具有 TTL，数据库提交后缓存失效；
- [ ] API/Web Pod 故障后业务入口保持可恢复。

### 28.5 当前尚不能声称完成的内容

- 后端尚未完成身份、账务、报表等独立微服务拆分；
- 单控制平面不具备控制面高可用；
- Local PV 不具备跨节点数据自动迁移；
- 单副本 MySQL/Redis 不具备中间件高可用；
- 当前 GitHub Actions 已具备 CI，但三节点内网集群的自动 CD 尚需自托管 Runner 或 GitOps 入口；
- Prometheus/Grafana/Loki 的 Kubernetes 原生部署需在资源扩容并完成 Chart 后单独验收。

这些内容会作为后续项目改造的验收目标，不能提前写入简历的“已完成”部分。

## 29. 官方依据

- Kubernetes 维护版本：https://kubernetes.io/releases/
- kubeadm 安装：https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/
- containerd 与 systemd cgroup：https://kubernetes.io/docs/setup/production-environment/container-runtimes/
- kubeadm 创建集群：https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/
- Kubernetes 探针：https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/
- Local Volume：https://kubernetes.io/docs/concepts/storage/volumes/#local
- StatefulSet：https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/
- NetworkPolicy：https://kubernetes.io/docs/concepts/services-networking/network-policies/
- Helm 安装：https://helm.sh/docs/intro/install/
- Helm Upgrade：https://helm.sh/docs/helm/helm_upgrade/
- Helm Rollback：https://helm.sh/docs/helm/helm_rollback/
- Calico 系统要求：https://docs.tigera.io/calico/latest/getting-started/kubernetes/requirements
- Calico 3.32.2 安装示例：https://docs.tigera.io/calico/latest/getting-started/kubernetes/quickstart
- CentOS Stream 镜像网络：https://docs.centos.org/infra-docs/buildsys/mirror-network/
