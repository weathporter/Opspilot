#!/usr/bin/env bash

# 在 Rocky Linux 上通过 Docker 官方 RPM 仓库安装 Engine、Buildx 与 Compose 插件。
set -Eeuo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# 可选把指定登录用户加入 docker 组；默认留空，坚持 sudo docker 的最小授权方式。
TARGET_USER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --user) TARGET_USER="${2:?--user需要参数}"; shift 2 ;;
    -h|--help)
      echo "用法: sudo bash scripts/install-docker.sh [--user 登录用户名]"
      exit 0
      ;;
    *) die "未知参数: $1" ;;
  esac
done

require_root
require_command dnf

# 脚本可重复执行：若 docker 已存在则不重复配置仓库和安装包。
if command -v docker >/dev/null 2>&1; then
  log "Docker已经安装: $(docker --version)"
else
  log "配置Docker官方RPM仓库"
  # config-manager 子命令由 dnf-plugins-core 提供。
  dnf -y install dnf-plugins-core
  if [[ ! -f /etc/yum.repos.d/docker-ce.repo ]]; then
    dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
  fi

  log "安装Docker Engine、Buildx和Compose插件"
  dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# enable --now 同时设置开机启动并立即启动 daemon。
systemctl enable --now docker
# 立即验证客户端能连 daemon，以及 Compose v2 插件可用。
docker version
docker compose version

if [[ -n "$TARGET_USER" ]]; then
  id "$TARGET_USER" >/dev/null 2>&1 || die "用户不存在: $TARGET_USER"
  usermod -aG docker "$TARGET_USER"
  # docker socket 可启动特权容器并挂载宿主目录，因此 docker 组事实上接近 root 权限。
  warn "已把 $TARGET_USER 加入docker组。该组具有接近root的权限，需要重新登录后生效。"
fi

log "Docker安装与启动完成"
