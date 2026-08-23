#!/usr/bin/env bash

# 统一执行 Maven 构建并验证只产生一个可发布 JAR。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# 默认运行全部测试；只有测试已经在同一代码版本上单独通过时才应使用 --skip-tests。
SKIP_TESTS=false
for argument in "$@"; do
  case "$argument" in
    --skip-tests) SKIP_TESTS=true ;;
    -h|--help)
      echo "用法: bash scripts/build.sh [--skip-tests]"
      exit 0
      ;;
    *) die "未知参数: $argument" ;;
  esac
done

# 无论从哪个目录调用都切到项目根，保证 Maven 和 target 路径稳定。
cd "$PROJECT_ROOT"
require_command mvn
require_command npm

# Bash 数组安全保存每个参数，避免字符串拼接后发生意外单词拆分。
build_command=(mvn -B -ntp clean package)
if [[ "$SKIP_TESTS" == true ]]; then
  build_command+=(-DskipTests)
  warn "本次构建跳过测试，只应在已单独验证测试时使用"
fi

log "开始构建 NorthLedger"
"${build_command[@]}"

# 前端与 JAR 属于同一个发布版本；使用锁文件还原依赖并禁用第三方安装脚本，再生成静态制品。
log "开始构建 React 生产资源"
(
  cd "${PROJECT_ROOT}/frontend"
  npm ci --ignore-scripts
  npm run build
)

# 排除 Spring Boot 重打包保留的 *.original，只接受唯一的 northledger-*.jar。
mapfile -t artifacts < <(find target -maxdepth 1 -type f -name 'northledger-*.jar' ! -name '*.original' -print)
[[ ${#artifacts[@]} -eq 1 ]] || die "预期找到一个可执行JAR，实际找到 ${#artifacts[@]} 个"
log "构建完成: ${PROJECT_ROOT}/${artifacts[0]}"
log "前端制品: ${PROJECT_ROOT}/frontend/dist"
