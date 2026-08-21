#!/usr/bin/env bash

# 可复用的 readiness 轮询器：发布、回滚和人工验证使用同一成功标准。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# 默认 18 次、每次间隔 5 秒，最长约 90 秒；单次 curl 另有 3 秒网络超时。
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:18080/actuator/health/readiness}"
ATTEMPTS=18
INTERVAL_SECONDS=5

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url) HEALTH_URL="${2:?--url需要参数}"; shift 2 ;;
    --attempts) ATTEMPTS="${2:?--attempts需要参数}"; shift 2 ;;
    --interval) INTERVAL_SECONDS="${2:?--interval需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: bash scripts/health-check.sh [--url URL] [--attempts N] [--interval 秒]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

# 在循环前校验数值，防止 0/负数/任意字符串让门禁被意外绕过。
[[ "$ATTEMPTS" =~ ^[1-9][0-9]*$ ]] || die "attempts必须为正整数"
[[ "$INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]] || die "interval必须为正整数"
require_command curl

for ((attempt = 1; attempt <= ATTEMPTS; attempt++)); do
  # --fail 把 4xx/5xx 视为失败；--max-time 防止单次请求无限等待。
  # 除 HTTP 成功外还检查 JSON 中 status=UP，避免 200 但业务状态异常被误判为健康。
  if response="$(curl --fail --silent --show-error --max-time 3 "$HEALTH_URL" 2>/dev/null)" \
      && grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' <<<"$response"; then
    log "健康检查通过: $HEALTH_URL"
    exit 0
  fi
  warn "健康检查未通过 (${attempt}/${ATTEMPTS})"
  # 最后一次失败后不再多睡一次，立即返回失败给发布/回滚脚本。
  if (( attempt < ATTEMPTS )); then
    sleep "$INTERVAL_SECONDS"
  fi
done

die "健康检查最终失败: $HEALTH_URL"
