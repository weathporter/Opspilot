#!/bin/sh

# Kubernetes 通过 Secret 注入明文密码；启动时只在内存 emptyDir 中写入 SHA-256 ACL 哈希。
set -eu
: "${REDIS_PASSWORD:?REDIS_PASSWORD must be provided by a Kubernetes Secret}"

umask 077
password_hash="$(printf '%s' "$REDIS_PASSWORD" | sha256sum | awk '{print $1}')"
printf 'user default on #%s ~* +@all\n' "$password_hash" > /run/redis/users.acl
unset REDIS_PASSWORD password_hash

exec redis-server \
  --aclfile /run/redis/users.acl \
  --appendonly yes \
  --appendfsync everysec \
  --dir /data \
  --maxmemory "${REDIS_MAXMEMORY:-128mb}" \
  --maxmemory-policy noeviction \
  --protected-mode yes
