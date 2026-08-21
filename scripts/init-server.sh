#!/usr/bin/env bash

# 在全新的 Rocky Linux 9 主机上准备原生部署所需的软件、用户、目录和系统配置。
set -Eeuo pipefail

# BASH_SOURCE[0] 指向当前脚本本身；先算绝对目录，确保从任意工作目录执行结果都一致。
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

# --skip-packages 适合软件已由镜像/配置管理系统安装的主机，避免重复 dnf。
SKIP_PACKAGES=false
for argument in "$@"; do
  case "$argument" in
    --skip-packages) SKIP_PACKAGES=true ;;
    -h|--help)
      echo "用法: sudo bash scripts/init-server.sh [--skip-packages]"
      exit 0
      ;;
    *) die "未知参数: $argument" ;;
  esac
done

# 后续会写 /etc、创建用户、调整 SELinux/firewalld，因此必须 root。
require_root

# 读取发行版身份；非 Rocky 只警告而非直接退出，方便兼容发行版实验，但需人工验证命令差异。
if [[ -r /etc/os-release ]]; then
  # shellcheck disable=SC1091
  source /etc/os-release
  [[ "${ID:-}" == "rocky" ]] || warn "当前系统是 ${PRETTY_NAME:-unknown}，脚本主要在Rocky Linux 9验证"
fi

if [[ "$SKIP_PACKAGES" == false ]]; then
  require_command dnf
  log "安装JDK、Nginx、MySQL客户端和基础排障工具"
  # headless JDK 不含桌面组件；curl/tar/gzip/rsync/mysql 用于检查、发布、备份和诊断。
  dnf install -y java-17-openjdk-headless nginx curl tar gzip rsync mysql policycoreutils-python-utils
fi

# 专用系统账号无交互 shell，降低服务凭据被用于登录的风险；重复执行脚本不会重复创建。
if ! id opspilot >/dev/null 2>&1; then
  useradd --system --home-dir /opt/opspilot --shell /sbin/nologin opspilot
  log "已创建专用服务账号 opspilot"
fi

# releases 存不可变版本，current/previous 是软链接；shared 存跨版本共享文件，日志单独放 /var/log。
install -d -o opspilot -g opspilot -m 0750 /opt/opspilot/releases /opt/opspilot/shared /var/log/opspilot
# 配置目录 root 可写、服务组可读，普通用户不可访问。
install -d -o root -g opspilot -m 0750 /etc/opspilot

# -D 会创建必要父目录；系统配置归 root 所有且普通用户只读。
install -D -o root -g root -m 0644 "${PROJECT_ROOT}/deploy/linux/systemd/opspilot.service" /etc/systemd/system/opspilot.service
install -D -o root -g root -m 0644 "${PROJECT_ROOT}/deploy/linux/nginx/opspilot.conf" /etc/nginx/conf.d/opspilot.conf
install -D -o root -g root -m 0644 "${PROJECT_ROOT}/deploy/linux/logrotate/opspilot" /etc/logrotate.d/opspilot

# 首次才复制环境模板，重复初始化不能覆盖管理员已经填写的真实密码。
if [[ ! -f /etc/opspilot/opspilot.env ]]; then
  install -o root -g opspilot -m 0640 "${PROJECT_ROOT}/deploy/linux/env/opspilot.env.example" /etc/opspilot/opspilot.env
  warn "已创建 /etc/opspilot/opspilot.env，请先修改DB_PASSWORD等配置"
fi

# SELinux enforcing/permissive 时允许 httpd_t 域的 Nginx 主动连接本机 Java upstream。
if command -v getenforce >/dev/null 2>&1 && [[ "$(getenforce)" != "Disabled" ]]; then
  setsebool -P httpd_can_network_connect 1
  log "已允许SELinux下的Nginx连接本地Java端口"
fi

# 只开放标准 HTTP 服务；Java 18080 继续绑定 127.0.0.1，不在防火墙对外暴露。
if systemctl is-active --quiet firewalld; then
  firewall-cmd --permanent --add-service=http
  firewall-cmd --reload
  log "已在firewalld开放HTTP服务；18080仍只监听本机"
fi

# 配置校验必须先于 reload/restart，防止错误 Nginx 文件使现有服务中断。
nginx -t
# 通知 systemd 重新扫描新增单元，然后设为开机启动；此时 JAR 尚未发布，所以只重启 Nginx。
systemctl daemon-reload
systemctl enable nginx opspilot
systemctl restart nginx

log "主机初始化完成"
log "下一步：编辑 /etc/opspilot/opspilot.env，然后执行 build.sh 与 deploy.sh"
