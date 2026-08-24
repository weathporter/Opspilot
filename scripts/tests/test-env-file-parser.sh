#!/usr/bin/env bash

# 验证 systemd EnvironmentFile 被当作数据解析，而不是被 source 为可执行 Shell。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../lib/common.sh
source "${SCRIPT_DIR}/../lib/common.sh"

test_env_file="$(mktemp "${TMPDIR:-/tmp}/northledger-env-parser.XXXXXX")"
trap 'rm -f -- "$test_env_file"' EXIT

printf '%s\n' \
  '# comment' \
  'REDIS_HOST=127.0.0.1' \
  'DISPLAY_NAME=System Administrator' \
  'LITERAL_META=$(printf should-not-execute);`printf neither`' \
  > "$test_env_file"

[[ "$(read_env_value "$test_env_file" DISPLAY_NAME)" == 'System Administrator' ]]
[[ "$(read_env_value "$test_env_file" LITERAL_META)" == '$(printf should-not-execute);`printf neither`' ]]

load_env_file "$test_env_file"
[[ "$DISPLAY_NAME" == 'System Administrator' ]]
[[ "$LITERAL_META" == '$(printf should-not-execute);`printf neither`' ]]

unset REDIS_HOST DISPLAY_NAME LITERAL_META
printf '%s\n' 'Environment file parser contract passed.'
