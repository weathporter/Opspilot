#!/usr/bin/env bash

# 受限故障注入工具：只允许在实验机验证 CPU/磁盘/停服务告警与自动恢复流程。
# 三种场景都设置严格持续时间/大小上限，并通过 EXIT trap 清理可恢复资源。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

SCENARIO=""
DURATION=30
SIZE_MB=128
CONFIRM=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scenario) SCENARIO="${2:?--scenario需要参数}"; shift 2 ;;
    --duration) DURATION="${2:?--duration需要参数}"; shift 2 ;;
    --size-mb) SIZE_MB="${2:?--size-mb需要参数}"; shift 2 ;;
    --confirm) CONFIRM="${2:?--confirm需要参数}"; shift 2 ;;
    -h|--help)
      echo "仅用于实验机：bash scripts/inject-failure.sh --scenario cpu|disk|stop-app --confirm LAB_ONLY"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

# 显式确认口令防止误执行；它不是生产授权，脚本本身仍只允许实验环境。
[[ "$CONFIRM" == "LAB_ONLY" ]] || die "故障注入只能用于实验机，必须传入 --confirm LAB_ONLY"
# 时长限制在 5~120 秒，既有机会触发告警 for 条件，又避免长时间影响主机。
[[ "$DURATION" =~ ^[0-9]+$ && "$DURATION" -ge 5 && "$DURATION" -le 120 ]] \
  || die "duration必须在5至120秒之间"
# 临时磁盘文件限制在 16~512 MB，防止脚本本身填满根分区。
[[ "$SIZE_MB" =~ ^[0-9]+$ && "$SIZE_MB" -ge 16 && "$SIZE_MB" -le 512 ]] \
  || die "size-mb必须在16至512之间"

case "$SCENARIO" in
  cpu)
    require_command timeout
    warn "将在一个CPU核心制造 ${DURATION} 秒忙循环"
    # timeout 到期会以非零结束子进程，这是预期行为，所以用 || true 继续完成脚本。
    timeout "${DURATION}s" bash -c 'while :; do :; done' || true
    ;;
  disk)
    require_command fallocate
    # 只在受控实验目录创建一个固定名称文件，EXIT trap 确保中断时也清理。
    lab_dir="/var/tmp/opspilot-fault-lab"
    lab_file="${lab_dir}/bounded-disk-fill.bin"
    install -d -m 0750 "$lab_dir"
    trap 'rm -f -- "$lab_file"' EXIT
    warn "将在 $lab_file 临时创建 ${SIZE_MB}MB 文件，${DURATION} 秒后自动删除"
    fallocate -l "${SIZE_MB}M" "$lab_file"
    sleep "$DURATION"
    ;;
  stop-app)
    require_root
    warn "将停止OpsPilot ${DURATION} 秒，然后自动启动"
    systemctl stop opspilot
    # 如果用户 Ctrl+C 或 sleep 失败，EXIT trap 仍尝试恢复应用。
    trap 'systemctl start opspilot >/dev/null 2>&1 || true' EXIT
    sleep "$DURATION"
    systemctl start opspilot
    ;;
  *) die "scenario必须是 cpu、disk 或 stop-app" ;;
esac

log "故障注入已结束，请观察告警恢复并记录排查过程"
