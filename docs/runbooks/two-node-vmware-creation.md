# NorthLedger 两节点 VMware 创建与复用手册

> 操作由你在 VMware/FinalShell 中完成，本文不是“已经部署成功”的记录。**优先复用**你现有的 `nl-cp1`、`nl-worker1`；仅在旧机器不可用时创建新克隆。`nl-worker2` 保留，不删除。

## 1. 资源规划与复用判断

宿主机约 15.2 GiB RAM，曾有一次测量仅余约 1.4 GiB；开机前请在 Windows 任务管理器重新确认。两台建议：`nl-cp1` 为 2 vCPU/3 GiB/40 GiB，`nl-worker1` 为 2～4 vCPU/5 GiB/至少 40 GiB（新建可考虑 60 GiB）。VM 内存共 8 GiB；关掉不用的 Docker Desktop 和大型程序后再开机。控制平面保持默认 `NoSchedule` 污点，业务与 MySQL/Redis 的 Local PV 都落在 worker1，因此不是高可用。

如果原两台已存在，先在 VMware 正常关机后调整 RAM，不必重新安装。cp1 上先检查：

```bash
kubectl get nodes -o wide
kubectl get pv -o wide
kubectl -n northledger get pvc,pods -o wide
```

若 `nl-worker2` 已加入集群并承载 Pod 或 Local PV，**不能直接关机或改 PV 节点亲和性**；先把输出发给我制定数据迁移/停机顺序。若 worker2 尚未加入且没有业务数据，让它关机备用即可。然后分别在现有 cp1/worker1 核对：

```bash
hostnamectl --static
ip -br -4 address
free -h
sudo systemctl is-active containerd
sudo systemctl is-enabled kubelet
sysctl net.ipv4.ip_forward
```

两机 IP/主机名须不同，`ip_forward` 应为 `1`。旧截图中 cp1 曾因 1674 MiB 内存及转发参数为 0 而未通过预检；那不代表当前状态。缺项按[CentOS Stream 9 节点安装手册](2026-09-22-centos-stream9-kubernetes-three-node.md)修复，不用 `--ignore-preflight-errors` 跳过真实问题。

## 2. 仅在需要新建时克隆模板

已知模板目录为 `E:\fff\develop\nl-k8s-template`。确认模板完全关机，且新副本目录与旧 VM、模板目录不同。VMware Workstation 可在模板机菜单 `VM → Manage → Clone` 选择 `Full clone`，分别创建 `nl-cp1`、`nl-worker1`；若菜单不可用，可在关机状态下通过资源管理器完整复制模板文件夹到两个新目录，再分别打开新的 `.vmx`。首次启动副本若问“移动还是复制”，选 **I Copied It/我已复制**，使 VMware 生成新的 UUID/MAC；[VMware 官方说明](https://knowledge.broadcom.com/external/article?legacyId=2030609)。原模板保留，不改名、不覆盖。

VMware 设置里将网卡选 `NAT / VMnet8`，按第 1 节分配 CPU/RAM。若模板磁盘只有 40 GiB，不必为了演示冒险扩有数据磁盘；新建 worker 若扩到 60 GiB，还须在 Linux 验证分区/文件系统实际扩容。每次只启动一个尚未设置 IP 的克隆，避免两个副本沿用模板的同一静态 IP。

克隆后 Linux 主机名、`machine-id`、SSH host keys、IP/网卡 MAC 也必须唯一；更改 VMware 显示名称不够。先在两个**新克隆机**的控制台比较 `cat /etc/machine-id`、`ip -br link`、`sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`。若有重复，先停下来处理身份，不要继续 `kubeadm init/join`；不要在已使用的旧节点上执行模板清理。RHEL 9 的[克隆文档](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/configuring_and_managing_virtualization/cloning-virtual-machines_configuring-and-managing-virtualization)也要求移除模板的唯一身份信息。

## 3. 两台机器配置独立网络

先在 VMware `Edit → Virtual Network Editor` 看 VMnet8 **实际**子网、NAT 网关、DHCP 地址池。以下地址仅在 VMnet8 仍是 `192.168.66.0/24`、网关 `192.168.66.2` 且 `.110/.111` 未被占用时使用。Linux 控制台运行 `nmcli -t -f NAME,DEVICE connection show --active`，将以下 `ens33` 换成实际**连接名**。`nmcli connection up` 会断开当前 SSH，首次改 IP 一定在 VMware 控制台做。

```bash
# 只在新 nl-cp1 中执行
sudo hostnamectl set-hostname nl-cp1
sudo nmcli connection modify 'ens33' ipv4.method manual ipv4.addresses 192.168.66.110/24 ipv4.gateway 192.168.66.2 ipv4.dns '192.168.66.2 1.1.1.1' ipv4.ignore-auto-dns yes
sudo nmcli connection up 'ens33'
```

```bash
# 只在新 nl-worker1 中执行
sudo hostnamectl set-hostname nl-worker1
sudo nmcli connection modify 'ens33' ipv4.method manual ipv4.addresses 192.168.66.111/24 ipv4.gateway 192.168.66.2 ipv4.dns '192.168.66.2 1.1.1.1' ipv4.ignore-auto-dns yes
sudo nmcli connection up 'ens33'
```

如果实际网段不同，同时替换地址和网关；不要让 VMnet8 与 Pod CIDR `10.244.0.0/16`、Service CIDR `10.96.0.0/12` 重叠。两机分别检查：

```bash
hostnamectl --static
ip -br -4 address
ip route
getent hosts pkgs.k8s.io
```

## 4. FinalShell 连接与验收

在每台 VM 的控制台启用 SSH：

```bash
sudo systemctl enable --now sshd
sudo systemctl is-active sshd
sudo ss -lntp | grep ':22 '
sudo firewall-cmd --state
```

若 `firewalld` 在运行但未放行 SSH，执行 `sudo firewall-cmd --permanent --add-service=ssh` 和 `sudo firewall-cmd --reload`；不要关闭整个防火墙。Windows PowerShell 用 `Test-NetConnection 192.168.66.110 -Port 22` 和 `.111` 验证连通。FinalShell 新建两条 SSH 连接，主机为 `.110`/`.111`，端口 22，用户为 `opsadmin`（以实际安装账号为准），密码或密钥由你保管。首次主机指纹与 VM 控制台的 `sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` 对照后再信任，不直接忽略指纹变化。

最后保留脱敏的配置截图、`hostnamectl`/IP/路由、`free -h`、`containerd` 与 `ip_forward` 结果。只有 `kubectl get nodes -o wide` 显示两节点均 `Ready` 且 CNI/CoreDNS 正常，才进入[两节点业务发布](two-node-microservices-release.md)。VM 能启动 ≠ Kubernetes 能运行 ≠ NorthLedger 业务闭环已通过。
