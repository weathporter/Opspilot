#!/usr/bin/env bash

# MySQL 恢复脚本。恢复会改写目标库，因此提供“明确库名确认 + dry-run + 恢复前安全备份”三层保护。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

ENV_FILE="${ENV_FILE:-/etc/opspilot/opspilot.env}"
BACKUP_FILE=""
CONFIRM_DATABASE=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file) ENV_FILE="${2:?--env-file需要参数}"; shift 2 ;;
    --file) BACKUP_FILE="${2:?--file需要参数}"; shift 2 ;;
    --confirm) CONFIRM_DATABASE="${2:?--confirm需要参数}"; shift 2 ;;
    --dry-run) DRY_RUN=true; shift ;;
    -h|--help)
      echo "用法: sudo bash scripts/restore-mysql.sh --file 备份.sql.gz --confirm 数据库名 [--dry-run]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

require_root
require_command mysql
require_command gzip
# --file 未提供时为空字符串，也会在这里明确失败。
require_file "$BACKUP_FILE"
load_env_file "$ENV_FILE"

: "${DB_HOST:?环境文件缺少DB_HOST}"
: "${DB_PORT:?环境文件缺少DB_PORT}"
: "${DB_NAME:?环境文件缺少DB_NAME}"
: "${DB_USERNAME:?环境文件缺少DB_USERNAME}"
: "${DB_PASSWORD:?环境文件缺少DB_PASSWORD}"

# 操作者必须原样输入目标 DB_NAME，防止拿错环境文件后直接把生产/测试库覆盖。
[[ "$CONFIRM_DATABASE" == "$DB_NAME" ]] || die "恢复会覆盖数据，必须显式传入 --confirm $DB_NAME"
# 先检查压缩文件结构；损坏备份不应进入任何写操作。
gzip -t "$BACKUP_FILE"

# dry-run 只验证参数、环境文件、目标库确认和 gzip 完整性，不连接 MySQL 写数据。
if [[ "$DRY_RUN" == true ]]; then
  log "检查通过：将把 $BACKUP_FILE 恢复到 $DB_NAME；dry-run未执行任何写入"
  exit 0
fi

# 真正恢复前对当前数据库再做一次备份，为误选恢复点提供最后撤销机会。
log "恢复前创建一次安全备份"
bash "${SCRIPT_DIR}/backup-mysql.sh" --env-file "$ENV_FILE"

# 临时 option file 隐藏密码，权限 0600，并在脚本退出时保证删除。
client_config="$(mktemp /tmp/opspilot-mysql-client.XXXXXX)"
trap 'rm -f -- "$client_config"' EXIT
chmod 0600 "$client_config"
printf '[client]\nhost=%s\nport=%s\nuser=%s\npassword=%s\n' \
  "$DB_HOST" "$DB_PORT" "$DB_USERNAME" "$DB_PASSWORD" >"$client_config"

log "开始恢复数据库 $DB_NAME"
# 流式解压并交给 mysql，避免在磁盘生成巨大的明文 SQL；pipefail 保证任一端失败都返回非零。
gzip -dc "$BACKUP_FILE" | mysql --defaults-extra-file="$client_config" "$DB_NAME"
log "数据库恢复完成，请立即执行应用健康检查和业务校验"
