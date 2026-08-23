#!/usr/bin/env bash

# Linux 原生回滚：默认切换到 previous，也可指定 releases 下的明确版本。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

APP_HOME="${APP_HOME:-/opt/opspilot}"
TARGET_VERSION=""
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:18080/actuator/health/readiness}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) TARGET_VERSION="${2:?--version需要参数}"; shift 2 ;;
    --health-url) HEALTH_URL="${2:?--health-url需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: sudo bash scripts/rollback.sh [--version 版本]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

require_root
require_command flock
require_command systemctl

# 与发布共用锁，确保 current/previous 不会被两个并行操作交叉覆盖。
exec 9>/run/lock/opspilot-deploy.lock
flock -n 9 || die "已有另一个发布或回滚任务在执行"

# readlink -f 解析软链接到真实版本目录；不存在时转为空字符串并给出业务错误。
CURRENT_TARGET="$(readlink -f "${APP_HOME}/current" 2>/dev/null || true)"
[[ -n "$CURRENT_TARGET" ]] || die "当前版本链接不存在"

if [[ -n "$TARGET_VERSION" ]]; then
  # 显式版本会参与路径拼接，先执行安全字符校验。
  validate_safe_name "$TARGET_VERSION" "版本号"
  TARGET="${APP_HOME}/releases/${TARGET_VERSION}"
else
  TARGET="$(readlink -f "${APP_HOME}/previous" 2>/dev/null || true)"
fi

# 只有包含预期 JAR 的版本目录才允许成为目标；禁止“回滚”到当前版本造成无意义重启。
[[ -n "$TARGET" && -f "${TARGET}/opspilot.jar" && -f "${TARGET}/web/index.html" ]] \
  || die "可回滚版本不完整或不存在: ${TARGET:-未设置}"
[[ "$TARGET" != "$CURRENT_TARGET" ]] || die "目标版本已经是当前版本"

# 先让 previous 记录回滚前版本，若目标不健康还可自动恢复。
atomic_symlink "$CURRENT_TARGET" "${APP_HOME}/previous"
atomic_symlink "$TARGET" "${APP_HOME}/current"
log "切换到版本目录: $TARGET"
systemctl restart opspilot

if HEALTH_URL="$HEALTH_URL" "${SCRIPT_DIR}/health-check.sh"; then
  log "回滚成功"
  exit 0
fi

# 回滚也必须有门禁；旧版本不一定健康，例如数据库结构或外部依赖已经变化。
warn "回滚目标不健康，恢复回滚前版本"
atomic_symlink "$CURRENT_TARGET" "${APP_HOME}/current"
systemctl restart opspilot
HEALTH_URL="$HEALTH_URL" "${SCRIPT_DIR}/health-check.sh" || true
die "回滚失败，需要人工排查"
