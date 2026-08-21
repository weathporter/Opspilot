#!/usr/bin/env bash

# 只读巡检脚本：按“主机资源 -> 网络 -> 服务 -> 日志 -> 配置 -> 健康”顺序快速收集现场信息。
# 单项失败大多使用 || true，保证即使某工具缺失也能继续输出后续证据。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:18080/actuator/health/readiness}"

# 用清晰分段标题让终端输出可直接粘贴进故障工单或复盘文档。
section() {
  printf '\n===== %s =====\n' "$1"
}

section "主机与时间"
# 主机名定位节点；时间用于对齐日志；uptime/load average 初判负载和最近重启。
hostnamectl 2>/dev/null || hostname
date --iso-8601=seconds
uptime

section "CPU与内存"
# free 看总体内存；vmstat 连续采样观察 CPU wait/换页；ps 找最耗 CPU 的进程。
free -h || true
vmstat 1 3 || true
ps -eo pid,ppid,user,%cpu,%mem,stat,etime,cmd --sort=-%cpu | head -n 12

section "磁盘与inode"
# 容量未满但 inode 用尽同样无法创建文件，所以两者都检查。
df -hT
df -ih

section "监听端口"
# 验证 80/18080/3306 等端口是否监听，以及由哪个进程占用。
ss -lntp || true

section "OpsPilot systemd状态"
# 查看 Active/退出码、主进程 PID、资源消耗和最近几条单元日志。
systemctl status opspilot --no-pager || true

section "最近应用日志"
# journalctl 能看到 systemd 启停与 Java 控制台日志；固定 80 行避免输出失控。
journalctl -u opspilot -n 80 --no-pager || true

section "Nginx配置"
# nginx -t 同时检查语法和引用文件可访问性，不真正重载服务。
nginx -t 2>&1 || true

section "应用健康"
# 直接访问 Java readiness，可区分应用自身故障与外部 Nginx 链路故障。
curl --silent --show-error --max-time 3 "$HEALTH_URL" || true
printf '\n'

section "Java版本"
# 核对运行机 JRE 是否与项目要求的 Java 17 兼容。
java -version 2>&1 || true
