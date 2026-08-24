#!/usr/bin/env bash

# 收集一次故障现场的主机、systemd、Nginx、日志和 Docker 快照，打包后供工单/复盘分析。
# 该脚本只读取系统状态，但诊断包仍可能包含主机名、进程参数和业务日志，发送前必须人工检查。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

OUTPUT_DIR="${OUTPUT_DIR:-/var/tmp}"
# journalctl 接受的相对时间表达式，可通过 --since 调整故障窗口。
SINCE="30 minutes ago"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output-dir) OUTPUT_DIR="${2:?--output-dir需要参数}"; shift 2 ;;
    --since) SINCE="${2:?--since需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: bash scripts/collect-diagnostics.sh [--output-dir 目录] [--since '30 minutes ago']"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

require_command tar
require_command sha256sum
install -d -m 0750 "$OUTPUT_DIR"

# mktemp 创建唯一工作目录；固定 /var/tmp 前缀且变量只来源于 mktemp，供 EXIT trap 安全清理。
work_dir="$(mktemp -d /var/tmp/opspilot-diagnostics.XXXXXX)"
trap 'rm -rf -- "$work_dir"' EXIT

# capture 把每条诊断命令的 stdout/stderr 保存到独立文件。
# 单项命令失败不终止整包收集，因为“命令失败/无权限”的输出本身也是排障证据。
capture() {
  local name="$1"
  shift
  "$@" >"${work_dir}/${name}.txt" 2>&1 || true
}

# 主机身份和运行时长用于确认故障节点、重启时间及负载背景。
capture hostname hostnamectl
capture uptime uptime
# 内存总体、短时 vmstat、文件系统容量与 inode 使用情况。
capture memory free -h
capture vmstat vmstat 1 3
capture filesystem df -hT
capture inode df -ih
# 监听端口和全量进程快照用于发现端口占用、僵尸进程和资源异常。
capture ports ss -lntp
capture processes ps -eo pid,ppid,user,%cpu,%mem,stat,etime,cmd
# 同时保存服务状态、实际合并后的 systemd 单元、Nginx 完整配置和指定窗口日志。
capture service systemctl status opspilot --no-pager
capture service-definition systemctl cat opspilot
capture nginx nginx -T
capture journal journalctl -u opspilot --since "$SINCE" --no-pager

# Redis INFO 不包含会话正文，但仍只采集运行、内存与统计分区；密码经环境传入且采集后立即清理。
capture_redis_info() {
  command -v redis-cli >/dev/null 2>&1 || return 0
  [[ -r /etc/opspilot/opspilot.env ]] || return 0
  local redis_host redis_port redis_username redis_password
  redis_host="$(read_env_value /etc/opspilot/opspilot.env REDIS_HOST || printf '127.0.0.1')"
  redis_port="$(read_env_value /etc/opspilot/opspilot.env REDIS_PORT || printf '6379')"
  redis_username="$(read_env_value /etc/opspilot/opspilot.env REDIS_USERNAME || printf 'default')"
  redis_password="$(read_env_value /etc/opspilot/opspilot.env REDIS_PASSWORD || true)"
  [[ -n "$redis_password" ]] || return 0
  REDISCLI_AUTH="$redis_password" redis-cli --no-auth-warning \
    --user "$redis_username" -h "$redis_host" -p "$redis_port" \
    INFO server memory stats
  unset redis_host redis_port redis_username redis_password
}
capture redis-info capture_redis_info

# 主机安装 Docker 时追加容器生命周期和资源瞬时快照；未安装时自然跳过。
if command -v docker >/dev/null 2>&1; then
  capture docker-ps docker ps -a
  capture docker-stats docker stats --no-stream
fi

# 仅保留环境变量键名，所有等号后的值统一替换为 <redacted>，避免数据库/Redis密码进入诊断包。
if [[ -r /etc/opspilot/opspilot.env ]]; then
  sed -E 's/^([A-Za-z_][A-Za-z0-9_]*)=.*/\1=<redacted>/' /etc/opspilot/opspilot.env \
    >"${work_dir}/environment-keys.txt"
fi

# 用时间戳命名归档，-C 后打包相对路径，避免压缩包泄露本机完整目录结构。
timestamp="$(date '+%Y%m%d_%H%M%S')"
archive="${OUTPUT_DIR}/opspilot-diagnostics_${timestamp}.tar.gz"
tar -czf "$archive" -C "$work_dir" .
# 校验和用于确认诊断包传输后未损坏或被篡改。
sha256sum "$archive" >"${archive}.sha256"
log "诊断包已生成: $archive"
log "环境变量值已脱敏，但发送前仍应人工检查内容"
