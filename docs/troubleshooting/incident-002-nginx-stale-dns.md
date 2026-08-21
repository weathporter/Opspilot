# 实际排障案例：应用容器重建后Nginx变为unhealthy

## 现象

- `docker compose up --build`成功重建应用容器。
- 新应用容器健康。
- 没有重建的Nginx容器随后无法访问应用并变为unhealthy。

## 根因

Docker Compose重建容器时可能分配新的容器IP。Nginx默认在启动时解析`app`服务名并缓存结果；应用IP变化后，持续运行的Nginx仍连接旧IP。

## 修复

Docker环境的Nginx使用Docker内置DNS并启用动态解析：

```nginx
resolver 127.0.0.11 valid=10s ipv6=off;

upstream opspilot_backend {
    zone opspilot_backend 64k;
    server app:18080 resolve;
}
```

## 验证

1. 记录当前应用容器IP。
2. 重建应用容器但不重启Nginx。
3. 确认新IP与旧IP不同。
4. 在DNS有效期后确认Nginx `/health`恢复200。

## 学习点

容器服务发现依赖DNS名称而不是固定IP；仅仅在Compose中写了服务名，不代表所有客户端都会持续重新解析DNS。
