#!/usr/bin/env bash

# 所有 Linux 脚本共用的安全选项与辅助函数；业务脚本通过 source 引入，避免复制粘贴产生行为漂移。

# -E：ERR trap 可传入函数；-e：命令失败立即退出；-u：未定义变量即报错；
# pipefail：管道中任一命令失败即判整个管道失败，而不是只看最后一个命令。
set -Eeuo pipefail

# 输出带时间和级别的普通进度日志，$* 把全部参数组合为一条消息。
log() {
  printf '%s [INFO] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

# 警告写到标准错误，便于调用方把正常输出和异常信息分流。
warn() {
  printf '%s [WARN] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
}

# 输出错误并返回非零退出码；systemd/CI/上层脚本可据此识别失败。
die() {
  printf '%s [ERROR] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
  exit 1
}

# 在真正执行变更前检查所需命令是否存在，给出比“command not found”更明确的前置条件错误。
require_command() {
  command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

# EUID=0 表示 root；安装、systemd、备份等系统级操作必须显式使用 sudo。
require_root() {
  [[ ${EUID} -eq 0 ]] || die "该操作需要root权限，请使用sudo重新执行"
}

# 对输入文件做存在性检查，避免后续命令在错误路径上产生更难理解的失败。
require_file() {
  [[ -f "$1" ]] || die "文件不存在: $1"
}

# 读取 systemd EnvironmentFile 中某一个键的原始值，不执行文件中的 Shell 语法。
# 该函数专供巡检等最小权限场景，只取所需 Redis 字段，避免把数据库和管理员密钥扩散给子进程。
read_env_value() {
  local env_file="$1"
  local requested_key="$2"
  local line key value
  require_file "$env_file"
  [[ "$requested_key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || die "环境变量键名不合法: $requested_key"

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      if [[ "$key" == "$requested_key" ]]; then
        printf '%s' "$value"
        return 0
      fi
    fi
  done < "$env_file"
  return 1
}

# 安全加载管理员维护的 KEY=VALUE 文件并 export 给备份/恢复等子进程。
# 不能 source systemd EnvironmentFile：密码中的 $()、反引号或分号不应被当作命令执行，带空格值也不应报错。
load_env_file() {
  local env_file="$1"
  local line key value
  require_file "$env_file"
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]] || die "环境文件存在无效行: $env_file"
    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"
    export "$key=$value"
  done < "$env_file"
}

# 限制版本号等将参与路径拼接的值，阻止斜杠、空格或 .. 等危险字符改变目标目录。
validate_safe_name() {
  local value="$1"
  local label="$2"
  [[ "$value" =~ ^[A-Za-z0-9._-]+$ ]] || die "$label 只能包含字母、数字、点、下划线和短横线"
}

# 先创建唯一临时链接，再用同文件系统 mv 原子替换正式链接；读者不会看到 current 处于半更新状态。
atomic_symlink() {
  local target="$1"
  local link_path="$2"
  # $$ 是当前 shell PID，可降低并行任务临时文件重名概率；外层 flock 还会阻止发布并发。
  local temporary_link="${link_path}.next.$$"
  ln -s "$target" "$temporary_link"
  mv -Tf "$temporary_link" "$link_path"
}
