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
# 验证 80/18080/3306/6379 等端口是否监听，以及由哪个进程占用。
ss -lntp || true

section "Redis连通与内存策略"
# 从 root 管理、服务组只读的环境文件取得连接信息；REDISCLI_AUTH 不会把密码写进命令参数。
if command -v redis-cli >/dev/null 2>&1 && [[ -r /etc/opspilot/opspilot.env ]]; then
  # 逐项解析所需字段，不 source 整份 systemd EnvironmentFile，也不把数据库/管理员密钥 export 给子进程。
  redis_host="$(read_env_value /etc/opspilot/opspilot.env REDIS_HOST || printf '127.0.0.1')"
  redis_port="$(read_env_value /etc/opspilot/opspilot.env REDIS_PORT || printf '6379')"
  redis_username="$(read_env_value /etc/opspilot/opspilot.env REDIS_USERNAME || printf 'default')"
  redis_password="$(read_env_value /etc/opspilot/opspilot.env REDIS_PASSWORD || true)"
  if [[ -n "$redis_password" ]]; then
    REDISCLI_AUTH="$redis_password" redis-cli --no-auth-warning \
      --user "$redis_username" -h "$redis_host" -p "$redis_port" PING || true
    REDISCLI_AUTH="$redis_password" redis-cli --no-auth-warning \
      --user "$redis_username" -h "$redis_host" -p "$redis_port" INFO memory \
      | grep -E '^(used_memory_human|maxmemory_human|maxmemory_policy):' || true
  else
    warn "REDIS_PASSWORD未配置，跳过认证检查"
  fi
  unset redis_host redis_port redis_username redis_password
else
  warn "redis-cli或受控环境文件不可用，跳过Redis协议检查"
fi

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
