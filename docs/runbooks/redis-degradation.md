# Redis 不可用与内存压力 Runbook

## 1. 影响范围

NorthLedger 0.3.0 的 Redis 保存两类数据：`northledger:session` 登录会话，以及 `northledger:cache:` 可重建总览缓存。MySQL 仍是账户、余额、订单、流水、用户和审计的唯一事实来源。

因此 Redis 故障有两种不同影响：

- 总览缓存 GET/PUT 失败时，服务层会记录 `cache_operation_degraded` 并回源 MySQL；页面查询压力和延迟可能上升。
- HTTP 请求在进入业务服务前需要读取 Redis 会话；Redis 完全不可用时，已登录用户可能收到认证/系统错误。不要用“缓存可以降级”掩盖会话依赖不可用。

## 2. 告警解释

| 告警 | 含义 | 首要动作 |
| --- | --- | --- |
| `RedisUnavailable` | exporter 无法认证或连接 Redis 持续 1 分钟 | 检查容器/Pod、Redis 日志、密码 Secret 和网络策略 |
| `RedisMemoryPressure` | `used_memory / maxmemory > 85%` 持续 5 分钟 | 检查键数量、TTL、会话增长与配置上限 |
| `RedisUnexpectedEviction` | 5 分钟内淘汰键增加 | 核对是否仍为 `noeviction`，确认未连错实例 |

## 3. Compose 排查顺序

所有命令在仓库根目录执行。不要把真实密码直接写进命令历史；Compose 容器内已经有受控环境变量。

```powershell
docker compose -f compose.yml -f compose.local.yml -f compose.observability.yml -f compose.desktop-observability.yml ps
docker compose logs --since 15m redis redis-exporter app
docker compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
docker compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli INFO memory'
docker compose exec -T redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli INFO stats'
```

预期 `PING` 返回 `PONG`。`INFO memory` 重点看 `used_memory`、`maxmemory`、`maxmemory_policy:noeviction`；`INFO stats` 重点看 `rejected_connections`、`evicted_keys`、`keyspace_hits`、`keyspace_misses`。

随后检查 Prometheus：

```text
redis_up{job="redis"}
up{job="redis"}
redis_memory_used_bytes / redis_memory_max_bytes
increase(redis_evicted_keys_total[5m])
sum by (operation) (rate(northledger_cache_errors_total[5m]))
```

## 4. Kubernetes 排查顺序

```bash
kubectl -n opspilot get pod,svc,pvc -l app.kubernetes.io/name=opspilot
kubectl -n opspilot describe pod -l app.kubernetes.io/component=redis
kubectl -n opspilot logs statefulset/opspilot-redis --since=15m
kubectl -n opspilot logs deployment/opspilot-redis-exporter --since=15m
kubectl -n opspilot get networkpolicy
```

若 API initContainer 持续等待，依次核对 Redis Service/Endpoint、Pod readiness、Secret 键名、NetworkPolicy 和 DNS，不要直接删除 PVC 或重建全部工作负载。

## 5. 恢复动作

1. 先保存 `ps/get pods`、日志、Redis INFO、Prometheus 时间线和最近发布版本。
2. 若只是进程退出，使用编排器正常重启并观察 readiness；不要先删除数据卷。
3. 若认证失败，对比应用、exporter 和 Redis 引用的 Secret 版本；按受控流程轮换后滚动重启客户端。
4. 若达到 `maxmemory`，先判断键是否都有合理 TTL。学习环境可提高上限，但必须同时检查主机可用内存；生产环境按平台容量和会话峰值扩容。
5. Redis 恢复后验证 PING、`up{job="redis"}=1`、`redis_up=1`、应用 readiness、重新登录、总览两次读取和缓存 TTL。
6. 等待告警恢复，记录开始时间、发现方式、影响、根因、恢复动作和防复发项。

### Kubernetes 凭据轮换的一致性窗口

`existingSecret` 由平台在 Helm 之外管理，Chart 无法根据其内容变化自动改动 Pod 模板。所以必须先记录维护窗口、资源名、旧凭据回退路径和验收人，再根据实际部署模式选择下列一条流程。

#### 模式 A：Chart 内置 Redis（本地/Minikube）

内置模式中 `redis.enabled=true`，运行 Secret 为 `opspilot-runtime`，且集群中真实存在 `statefulset/opspilot-redis`。必须让 Secret、Redis、API 和 exporter 使用同一代凭据，然后按顺序恢复：

```bash
kubectl -n opspilot rollout restart statefulset/opspilot-redis
kubectl -n opspilot rollout status statefulset/opspilot-redis --timeout=180s
kubectl -n opspilot rollout restart deployment/opspilot-api deployment/opspilot-redis-exporter
kubectl -n opspilot rollout status deployment/opspilot-api --timeout=180s
kubectl -n opspilot rollout status deployment/opspilot-redis-exporter --timeout=180s
```

上述命令只适用内置 Redis，执行前必须确认 StatefulSet 存在。

#### 模式 B：企业外部高可用 Redis

类生产 values 中 `redis.enabled=false`，不存在 `statefulset/opspilot-redis`。运行凭据必须更新 `secrets.existingSecret` 指定的对象；本项目示例是 `opspilot-production-runtime`，真实环境必须以实际 values 为准。

1. 由企业 Redis 平台按其 ACL/双凭据或维护窗口流程先准备新凭据，保留可回退的旧凭据；本项目不猜测厂商命令。
2. 通过企业 Secret 管理流程更新实际 `existingSecret` 的 `redis-password`，不在终端参数或 Git 中写明文。
3. 不要操作不存在的内置 StatefulSet；只滚动依赖该外部凭据的客户端：

```bash
kubectl -n opspilot rollout restart deployment/opspilot-api deployment/opspilot-redis-exporter
kubectl -n opspilot rollout status deployment/opspilot-api --timeout=180s
kubectl -n opspilot rollout status deployment/opspilot-redis-exporter --timeout=180s
```

4. 新客户端验证通过后再由 Redis 平台撤销旧凭据；若平台不支持双凭据，就必须在已批准的中断窗口内同步执行并准备回退。

两种模式都不能“只改 Secret 就结束”。完成后必须检查 Redis `PING`、API readiness、新登录会话、总览缓存 TTL、`redis_up=1` 和告警恢复；任一步失败就按事先保留的旧凭据回退。

## 6. 禁止动作

- 不执行 `FLUSHALL`、`FLUSHDB` 或模糊 `DEL *`；这会强制所有用户退出。
- 不把 Redis 端口改为 `0.0.0.0` 暴露公网来“临时排障”。
- 不在终端参数、截图、工单、日志或 Git 中粘贴真实 Redis 密码。
- 不把 `maxmemory-policy` 改成随机淘汰策略；共享实例中的会话不能被当作普通缓存静默丢弃。
- 不把单节点学习部署描述成生产高可用；生产必须使用受控外部高可用 Redis。
