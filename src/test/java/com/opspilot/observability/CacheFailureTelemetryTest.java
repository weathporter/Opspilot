package com.opspilot.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** 验证缓存降级处理本身不会把 Redis 异常重新抛给业务调用方。 */
class CacheFailureTelemetryTest {

    @Test
    void everyCacheOperationFailureIsSwallowedAndCountedWithBoundedTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CacheFailureTelemetry telemetry = new CacheFailureTelemetry(meterRegistry);
        ConcurrentMapCache cache = new ConcurrentMapCache("operations-summary");
        RuntimeException redisFailure = new RuntimeException("test-only Redis outage");

        assertThatCode(() -> telemetry.handleCacheGetError(redisFailure, cache, "current"))
                .doesNotThrowAnyException();
        assertThatCode(() -> telemetry.handleCachePutError(redisFailure, cache, "current", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> telemetry.handleCacheEvictError(redisFailure, cache, "current"))
                .doesNotThrowAnyException();
        assertThatCode(() -> telemetry.handleCacheClearError(redisFailure, cache))
                .doesNotThrowAnyException();

        for (String operation : new String[]{"get", "put", "evict", "clear"}) {
            assertThat(meterRegistry.get("northledger.cache.errors")
                    .tag("operation", operation)
                    .tag("cache", "operations-summary")
                    .counter()
                    .count()).isEqualTo(1.0);
        }
    }
}
