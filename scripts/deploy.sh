#!/usr/bin/env bash

# Linux 原生发布脚本：创建不可变版本目录、原子切换 current、重启并以 readiness 作为发布门禁。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# APP_HOME 可供测试环境覆盖；生产默认遵循 /opt 下第三方应用目录约定。
APP_HOME="${APP_HOME:-/opt/opspilot}"
# 未指定 artifact 时自动从 Maven target 中寻找唯一 JAR。
ARTIFACT=""
# React 生产目录与 JAR 一起写入同一个不可变 release，保证升级/回滚不会前后端错版。
WEB_DIST=""
# 默认版本精确到秒，既可排序又不会覆盖历史发布；正式流水线也可传 Git commit/tag。
VERSION="$(date '+%Y%m%d%H%M%S')"
# 直连本机 Java readiness，排除外部网络波动对发布判断的影响。
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:18080/actuator/health/readiness}"

# 逐个消费参数；带值选项 shift 2，开关/帮助按各自分支退出。
while [[ $# -gt 0 ]]; do
  case "$1" in
    --artifact) ARTIFACT="${2:?--artifact需要参数}"; shift 2 ;;
    --version) VERSION="${2:?--version需要参数}"; shift 2 ;;
    --web-dist) WEB_DIST="${2:?--web-dist需要参数}"; shift 2 ;;
    --health-url) HEALTH_URL="${2:?--health-url需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: sudo bash scripts/deploy.sh [--artifact JAR] [--web-dist DIR] [--version 版本] [--health-url URL]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

# 发布需要写 /opt、切 systemd 服务和 /run 锁文件，因此必须 root。
require_root
require_command flock
require_command sha256sum
require_command systemctl
# 版本号会参与目录拼接，必须先限制字符，防止路径穿越或意外创建嵌套目录。
validate_safe_name "$VERSION" "版本号"

# 自动发现时只接受一个 Spring Boot 可执行 JAR，避免把旧包或 *.original 发到生产。
if [[ -z "$ARTIFACT" ]]; then
  mapfile -t artifacts < <(find "${PROJECT_ROOT}/target" -maxdepth 1 -type f -name 'northledger-*.jar' ! -name '*.original' -print)
  [[ ${#artifacts[@]} -eq 1 ]] || die "请先构建，或通过--artifact指定唯一JAR"
  ARTIFACT="${artifacts[0]}"
fi
require_file "$ARTIFACT"

# 未显式指定时使用项目的标准 Vite 输出；index.html 是最小完整性标志。
if [[ -z "$WEB_DIST" ]]; then
  WEB_DIST="${PROJECT_ROOT}/frontend/dist"
fi
require_file "${WEB_DIST}/index.html"
require_command rsync

# 文件描述符 9 持有非阻塞排他锁；脚本退出时 fd 关闭并自动释放。
# deploy 与 rollback 共用同一把锁，防止两个任务同时切换 current/previous。
exec 9>/run/lock/opspilot-deploy.lock
flock -n 9 || die "已有另一个发布或回滚任务在执行"

# 每个版本使用独立不可变目录；发现同名就拒绝，避免覆盖正在审计或可回滚的旧包。
RELEASE_DIR="${APP_HOME}/releases/${VERSION}"
[[ ! -e "$RELEASE_DIR" ]] || die "版本目录已存在: $RELEASE_DIR"
install -d -o opspilot -g opspilot -m 0750 "$RELEASE_DIR"
install -o opspilot -g opspilot -m 0640 "$ARTIFACT" "${RELEASE_DIR}/opspilot.jar"

# Nginx 以 opspilot 补充组读取静态资源；目录可遍历、文件只读，禁止 Web 进程修改发布制品。
WEB_RELEASE_DIR="${RELEASE_DIR}/web"
install -d -o opspilot -g opspilot -m 0750 "$WEB_RELEASE_DIR"
rsync --archive --delete --chown=opspilot:opspilot --chmod=D750,F640 "${WEB_DIST}/" "${WEB_RELEASE_DIR}/"

# 生成 SHA-256 校验文件，后续可验证 JAR 在传输/存储过程中是否损坏或被替换。
sha256sum "${RELEASE_DIR}/opspilot.jar" >"${RELEASE_DIR}/opspilot.jar.sha256"
chown opspilot:opspilot "${RELEASE_DIR}/opspilot.jar.sha256"
# 对前端入口也保存校验值，排查页面与版本不一致时可以快速核对制品。
sha256sum "${WEB_RELEASE_DIR}/index.html" >"${WEB_RELEASE_DIR}/index.html.sha256"
chown opspilot:opspilot "${WEB_RELEASE_DIR}/index.html.sha256"

# 发布前记录 current 的真实目标，并让 previous 指向它，作为自动回退候选。
PREVIOUS_TARGET=""
if [[ -L "${APP_HOME}/current" ]]; then
  PREVIOUS_TARGET="$(readlink -f "${APP_HOME}/current")"
  atomic_symlink "$PREVIOUS_TARGET" "${APP_HOME}/previous"
fi
# 原子切换 current；systemd 的 WorkingDirectory/ExecStart 随后解析到新版本。
atomic_symlink "$RELEASE_DIR" "${APP_HOME}/current"

log "正在发布版本 $VERSION"
systemctl restart opspilot

# health-check 内部重试最长约 90 秒，给 JVM、Flyway 和数据库连接留出启动时间。
if HEALTH_URL="$HEALTH_URL" "${SCRIPT_DIR}/health-check.sh"; then
  log "发布成功，当前版本: $VERSION"
  exit 0
fi

# 新版本未就绪时自动切回旧链接并再次验证，失败版本目录保留供日志/JAR 比对。
warn "新版本健康检查失败，开始恢复上一版本"
if [[ -n "$PREVIOUS_TARGET" && -d "$PREVIOUS_TARGET" ]]; then
  atomic_symlink "$PREVIOUS_TARGET" "${APP_HOME}/current"
  systemctl restart opspilot
  HEALTH_URL="$HEALTH_URL" "${SCRIPT_DIR}/health-check.sh" || warn "上一版本也未恢复健康，需要人工排查"
else
  # 首次发布没有旧版本可恢复，停止不健康服务，避免继续接收流量。
  systemctl stop opspilot || true
fi
die "发布失败，失败版本保留在 $RELEASE_DIR 供排查"
