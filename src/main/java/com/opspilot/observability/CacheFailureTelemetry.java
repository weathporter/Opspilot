package com.opspilot.observability;

import com.opspilot.dashboard.DashboardCacheNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

/**
 * 把 Redis 业务缓存错误转换为“继续回源 + 可观测证据”。
 *
 * <p>Spring 默认 {@code SimpleCacheErrorHandler} 会重新抛出异常；对 NorthLedger 总览缓存而言，
 * Redis 不是业务事实来源，读失败应表现为未命中并执行 MySQL 查询，写/清理失败则由短 TTL 兜底。
 * 官方 {@code AbstractCacheInvoker} 约定：GET 处理器不抛异常时按缓存未命中继续加载。
 * 依据：https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/cache/annotation/CachingConfigurer.html</p>
 */
@Component
public class CacheFailureTelemetry implements CacheErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheFailureTelemetry.class);

    /** 未知名称归一化，防止未来动态缓存名把 Micrometer 标签基数无限放大。 */
    private static final String UNKNOWN_CACHE = "unknown";

    private final MeterRegistry meterRegistry;

    public CacheFailureTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        record("get", exception, cache);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        record("put", exception, cache);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        record("evict", exception, cache);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        record("clear", exception, cache);
    }

    /**
     * 只记录固定操作、受控缓存名和异常类型；缓存键、值、Cookie、用户名和异常消息都可能包含
     * 敏感或高基数数据，因此既不进入日志字段，也不进入 Prometheus 标签。
     */
    private void record(String operation, RuntimeException exception, Cache cache) {
        String boundedCacheName = DashboardCacheNames.OPERATIONS_SUMMARY.equals(cache.getName())
                ? DashboardCacheNames.OPERATIONS_SUMMARY
                : UNKNOWN_CACHE;

        Counter.builder("northledger.cache.errors")
                .description("Handled Redis business-cache operation failures")
                .tag("operation", operation)
                .tag("cache", boundedCacheName)
                .register(meterRegistry)
                .increment();

        LOGGER.atWarn()
                .addKeyValue("event", "cache_operation_degraded")
                .addKeyValue("operation", operation)
                .addKeyValue("cache", boundedCacheName)
                .addKeyValue("exception_type", exception.getClass().getSimpleName())
                .log("Redis business cache operation failed; continuing with bounded fallback");
    }
}
