import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * 本地开发时由 Vite 把 /api 和 /health 转发到 Spring Boot。
 * 容器化部署后同样的相对路径由 Nginx 转发，因此前端代码无需区分环境或写死主机名。
 */
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:18080',
      '/health': {
        target: 'http://localhost:18080',
        rewrite: () => '/actuator/health/readiness',
      },
    },
  },
});
