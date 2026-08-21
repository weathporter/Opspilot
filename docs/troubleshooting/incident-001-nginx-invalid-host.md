# 实际排障案例：Nginx健康检查返回400

## 现象

- Spring Boot readiness直连返回200。
- Nginx `/health`返回400。
- Nginx容器被标记为unhealthy。

## 证据

应用日志出现：

```text
The host [opspilot_backend] is not valid
The character [_] is never valid in a domain name.
```

## 推理

应用直连健康，说明Java进程和MySQL依赖正常。400来自Tomcat请求解析而不是业务控制器。Nginx健康检查location没有显式设置`Host`，于是把upstream组名`opspilot_backend`作为Host传入；下划线不符合域名规则。

## 修复

在`location = /health`增加：

```nginx
proxy_set_header Host $host;
```

重启Nginx后，`/health`返回200且容器状态变为healthy。

## 预防

- 所有反向代理location显式设置Host和请求ID。
- 自动化验收同时检查后端直连和Nginx入口。
- 不只看“容器Up”，还检查Docker health状态和真实HTTP返回。
