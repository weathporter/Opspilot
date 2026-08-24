package com.opspilot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.dashboard.DashboardCacheNames;
import com.opspilot.dashboard.OperationsSummaryResponse;
import com.opspilot.observability.CacheFailureTelemetry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NorthLedger 的 Redis 业务缓存边界。
 *
 * <p>{@code @EnableCaching} 放在独立配置类而不是应用启动类上，符合 Spring Boot 的建议：
 * 需要缓存的完整应用加载本配置，窄范围测试则可以排除或替换它。当前只声明一个有真实业务理由的
 * 总览缓存，不建立“以后可能会用”的空缓存。</p>
 *
 * <p>官方依据：
 * https://docs.spring.io/spring-boot/3.5/reference/io/caching.html 与
 * https://docs.spring.io/spring-data/redis/reference/3.5/redis/redis-cache.html</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisCacheConfiguration implements CachingConfigurer {

    /** 所有业务缓存键的稳定前缀；Spring Session 使用另一命名空间。 */
    private static final String CACHE_KEY_PREFIX = "northledger:cache:";

    /** 总览允许 30 秒最终一致，额外 10 秒抖动用于分散多实例同时回源。 */
    private static final Duration SUMMARY_BASE_TTL = Duration.ofSeconds(30);
    private static final Duration SUMMARY_MAX_JITTER = Duration.ofSeconds(10);

    /** CachingConfigurer 会较早初始化，官方建议用 ObjectProvider 延迟取得普通业务依赖。 */
    private final ObjectProvider<CacheFailureTelemetry> cacheFailureTelemetryProvider;

    public RedisCacheConfiguration(ObjectProvider<CacheFailureTelemetry> cacheFailureTelemetryProvider) {
        this.cacheFailureTelemetryProvider = cacheFailureTelemetryProvider;
    }

    /**
     * 建立只包含明确缓存名的 RedisCacheManager。
     *
     * <p>值使用指定 DTO 类型的 JSON 序列化器，而非 Java 原生序列化。这样 redis-cli 中的数据
     * 更可读，也避免对缓存字节流启用通用 Java 反序列化。应用 ObjectMapper 已带 Java Time 等
     * Spring Boot 模块，能够正确处理 LocalDateTime 和嵌套响应记录。</p>
    */
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        Jackson2JsonRedisSerializer<OperationsSummaryResponse> summarySerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, OperationsSummaryResponse.class);

        org.springframework.data.redis.cache.RedisCacheConfiguration summaryConfiguration =
                org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                        .computePrefixWith(cacheName -> CACHE_KEY_PREFIX + cacheName + "::")
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(summarySerializer))
                        .disableCachingNullValues()
                        .entryTtl(new JitteredTtlFunction(SUMMARY_BASE_TTL, SUMMARY_MAX_JITTER));

        return RedisCacheManager.builder(connectionFactory)
                // 预先创建缓存，Actuator/Micrometer 在启动时即可绑定统计，而非等待第一次请求。
                .withInitialCacheConfigurations(Map.of(
                        DashboardCacheNames.OPERATIONS_SUMMARY,
                        summaryConfiguration
                ))
                // 本地统计为 CacheMetrics 提供命中/未命中证据，Redis 服务指标由 exporter 提供。
                .enableStatistics()
                // 若调用方处于事务中，缓存写入/清理延迟到提交，避免回滚后留下错误缓存状态。
                .transactionAware()
                .build();
    }

    /**
     * 注解驱动缓存统一使用可观测降级处理器；若未覆盖，Spring 默认会把 Redis 异常抛给调用方。
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return cacheFailureTelemetryProvider.getObject();
    }

    /**
     * 为每个新缓存条目计算“基础 TTL + 随机抖动”。
     *
     * <p>Spring Data Redis 的 {@link RedisCacheWriter.TtlFunction} 在写入时调用，因此每个条目
     * 获得独立过期时间。随机数不是安全凭据，只用于流量摊平，使用线程本地生成器可避免共享锁。</p>
     */
    public static final class JitteredTtlFunction implements RedisCacheWriter.TtlFunction {

        private final Duration baseTtl;
        private final long maxJitterMillis;

        public JitteredTtlFunction(Duration baseTtl, Duration maxJitter) {
            if (baseTtl.isNegative() || baseTtl.isZero()) {
                throw new IllegalArgumentException("baseTtl must be positive");
            }
            if (maxJitter.isNegative()) {
                throw new IllegalArgumentException("maxJitter must not be negative");
            }
            this.baseTtl = baseTtl;
            this.maxJitterMillis = maxJitter.toMillis();
        }

        @Override
        public Duration getTimeToLive(Object key, Object value) {
            long jitterMillis = maxJitterMillis == 0
                    ? 0
                    : ThreadLocalRandom.current().nextLong(maxJitterMillis + 1);
            return baseTtl.plusMillis(jitterMillis);
        }
    }
}
