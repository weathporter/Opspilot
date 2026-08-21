# 第一阶段使用固定大版本的 Node 镜像安装依赖并生成纯静态资源。
FROM node:22-alpine AS builder
WORKDIR /workspace

# 先复制依赖清单以利用 Docker 分层缓存；源码变化不会导致每次重新安装 npm 包。
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# 再复制前端源码并执行 TypeScript 检查和 Vite 生产构建。
COPY frontend/ ./
RUN npm run build

# 第二阶段只保留 Nginx 和 dist，不携带 Node、源码或开发依赖，降低镜像体积与攻击面。
FROM nginx:1.28-alpine
COPY deploy/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/dist /usr/share/nginx/html

# 官方镜像中的 101 是 nginx 用户。预先调整目录归属后，以非 root 身份监听 8080。
RUN chown -R 101:101 /var/cache/nginx /var/run /etc/nginx/conf.d /usr/share/nginx/html
USER 101:101
EXPOSE 8080

# 健康检查既验证 Nginx 自身，也通过 /health 验证到 Spring Boot readiness 的代理链路。
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
