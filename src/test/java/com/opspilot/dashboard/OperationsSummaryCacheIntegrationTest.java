package com.opspilot.dashboard;

import com.opspilot.NorthLedgerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过真实 Redis 验证运维总览缓存的外部行为。
 *
 * <p>若缓存被误删、键前缀变化、TTL 丢失或切回进程内 Map，这组断言会失败；测试没有模拟
 * CacheManager，因此能覆盖 Spring AOP、JSON 序列化、Lettuce 连接和 Redis 过期时间整条链路。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class OperationsSummaryCacheIntegrationTest extends NorthLedgerIntegrationTest {

    private static final String SUMMARY_CACHE_KEY =
            "northledger:cache:operations-summary::current";

    @Autowired
    private OperationsDashboardApplicationService dashboardService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void removeSummaryFromPreviousTest() {
        // 只删除本测试拥有的固定键，不执行会误删 Spring Session 数据的 FLUSHALL。
        redisTemplate.delete(SUMMARY_CACHE_KEY);
    }

    @Test
    void repeatedSummaryReadUsesRedisSnapshotWithBoundedTtl() {
        OperationsSummaryResponse first = dashboardService.getSummary();
        OperationsSummaryResponse second = dashboardService.getSummary();

        assertThat(second.generatedAt())
                .as("TTL 内第二次读取必须复用第一次生成的快照")
                .isEqualTo(first.generatedAt());
        assertThat(redisTemplate.hasKey(SUMMARY_CACHE_KEY))
                .as("总览必须写入带明确命名空间的 Redis 键")
                .isTrue();

        Long ttlSeconds = redisTemplate.getExpire(SUMMARY_CACHE_KEY);
        assertThat(ttlSeconds)
                .as("30～40 秒设计 TTL 只允许扣除读取与整秒取整的极短耗时")
                .isBetween(29L, 40L);
    }
}
