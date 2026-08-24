#!/bin/sh

# 从环境变量生成只含 SHA-256 哈希的 Redis ACL 文件，然后降权启动 redis-server。
# 这样真实密码不会出现在进程参数或持久化卷中；Docker 管理员仍可查看容器环境，因此生产环境
# 应改用平台 Secret/TLS，而不能把本地 Compose 当作完整密钥管理系统。
set -eu

: "${REDIS_PASSWORD:?REDIS_PASSWORD must be provided by the deployment secret}"

if [ "$(id -u)" = "0" ]; then
  # 命名卷首次创建时属于 root；只把 Redis 数据目录和临时 ACL 目录交给镜像内固定 redis 用户。
  mkdir -p /run/redis
  chown 999:1000 /data /run/redis
  chmod 0700 /run/redis
  exec setpriv --reuid=999 --regid=1000 --clear-groups /bin/sh "$0" "$@"
fi

umask 077
password_hash="$(printf '%s' "$REDIS_PASSWORD" | sha256sum | awk '{print $1}')"
printf 'user default on #%s ~* +@all\n' "$password_hash" > /run/redis/users.acl

# 子进程只读取 ACL 哈希文件；清掉当前 shell 变量并且不把明文密码放进 redis-server 参数。
unset REDIS_PASSWORD password_hash

exec redis-server \
  --aclfile /run/redis/users.acl \
  --appendonly yes \
  --appendfsync everysec \
  --dir /data \
  --maxmemory "${REDIS_MAXMEMORY:-128mb}" \
  --maxmemory-policy noeviction \
  --protected-mode yes
