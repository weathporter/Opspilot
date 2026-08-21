#!/usr/bin/env bash

# MySQL 逻辑备份：生成压缩 SQL、完整性校验和，并按保留天数清理历史文件。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# 环境文件同时服务于应用和备份脚本，避免维护两套数据库连接信息。
ENV_FILE="${ENV_FILE:-/etc/opspilot/opspilot.env}"
# 备份不与应用版本/JAR 混放，方便单独挂载备份盘和设置权限。
BACKUP_DIR="${BACKUP_DIR:-/var/backups/opspilot/mysql}"
RETENTION_DAYS=14

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file) ENV_FILE="${2:?--env-file需要参数}"; shift 2 ;;
    --output-dir) BACKUP_DIR="${2:?--output-dir需要参数}"; shift 2 ;;
    --retention-days) RETENTION_DAYS="${2:?--retention-days需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: sudo bash scripts/backup-mysql.sh [--env-file 文件] [--output-dir 目录] [--retention-days 天数]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

# 默认目录位于 /var/backups，且需读取受限环境文件，因此要求 root。
require_root
require_command mysqldump
require_command gzip
require_command sha256sum
# 禁止 0、负数或非数字导致保留策略失控。
[[ "$RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]] || die "retention-days必须为正整数"
load_env_file "$ENV_FILE"

# ${VAR:?message} 在变量缺失/空值时立即退出，避免误连默认库或生成无效备份。
: "${DB_HOST:?环境文件缺少DB_HOST}"
: "${DB_PORT:?环境文件缺少DB_PORT}"
: "${DB_NAME:?环境文件缺少DB_NAME}"
: "${DB_USERNAME:?环境文件缺少DB_USERNAME}"
: "${DB_PASSWORD:?环境文件缺少DB_PASSWORD}"
# MySQL option file 以行分隔键值，密码含换行会破坏结构，因此明确拒绝。
[[ "$DB_PASSWORD" != *$'\n'* ]] || die "DB_PASSWORD不能包含换行符"

# 目录 root 可管理、opspilot 组可读取，其他用户无权列出备份。
install -d -o root -g opspilot -m 0750 "$BACKUP_DIR"

# 临时客户端配置权限为 0600，避免把密码放进命令行（可能被 ps 看到）或 MYSQL_PWD 环境变量。
client_config="$(mktemp /tmp/opspilot-mysql-client.XXXXXX)"
# partial 文件用于“先完整写完、再原子改名”；失败时不会留下看似正常的 .sql.gz。
temporary_backup=""
cleanup() {
  rm -f -- "$client_config"
  [[ -z "$temporary_backup" ]] || rm -f -- "$temporary_backup"
}
# 无论成功、命令失败还是收到退出信号，shell 退出时都清理敏感/半成品文件。
trap cleanup EXIT
chmod 0600 "$client_config"
printf '[client]\nhost=%s\nport=%s\nuser=%s\npassword=%s\n' \
  "$DB_HOST" "$DB_PORT" "$DB_USERNAME" "$DB_PASSWORD" >"$client_config"

# 时间戳放入文件名，便于人工排序和恢复时选择时间点。
timestamp="$(date '+%Y%m%d_%H%M%S')"
backup_file="${BACKUP_DIR}/${DB_NAME}_${timestamp}.sql.gz"
temporary_backup="${backup_file}.partial"

log "开始备份数据库 $DB_NAME"
# --single-transaction 在 InnoDB 上获取一致性快照且不长时间锁表；--quick 逐行流式读取控制内存。
# routines/triggers/events 补齐表数据之外的数据库对象；关闭 GTID 写入提高跨环境恢复兼容性。
# set -o pipefail 保证 mysqldump 或 gzip 任一失败都会让脚本失败。
mysqldump --defaults-extra-file="$client_config" \
  --single-transaction --quick --routines --triggers --events \
  --set-gtid-purged=OFF "$DB_NAME" | gzip -9 >"$temporary_backup"

# gzip -t 在发布备份文件前验证压缩流没有截断/损坏。
gzip -t "$temporary_backup"
# 同一目录内 mv 是原子切换，其他任务不会读取到仍在写入的正式文件名。
mv "$temporary_backup" "$backup_file"
temporary_backup=""

# 校验和用于传输后/恢复前核对文件完整性；备份正文和校验文件都限制为 0640。
sha256sum "$backup_file" >"${backup_file}.sha256"
chmod 0640 "$backup_file" "${backup_file}.sha256"

# 只在指定目录第一层删除符合当前数据库命名规则且超过保留期的备份/校验文件。
find "$BACKUP_DIR" -maxdepth 1 -type f \
  \( -name "${DB_NAME}_*.sql.gz" -o -name "${DB_NAME}_*.sql.gz.sha256" \) \
  -mtime "+${RETENTION_DAYS}" -delete

log "备份完成: $backup_file"
