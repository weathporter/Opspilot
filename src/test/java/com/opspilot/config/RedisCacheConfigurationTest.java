package com.opspilot.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** 纯单元测试验证 TTL 抖动的数学边界，不依赖 Docker 或系统时钟。 */
class RedisCacheConfigurationTest {

    @Test
    void jitteredTtlNeverLeavesConfiguredThirtyToFortySecondWindow() {
        RedisCacheConfiguration.JitteredTtlFunction ttlFunction =
                new RedisCacheConfiguration.JitteredTtlFunction(
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(10)
                );

        // 多次采样能捕获单位换算、负数和错误上界；期望范围来自规格而非实现常量。
        for (int sample = 0; sample < 1_000; sample++) {
            Duration ttl = ttlFunction.getTimeToLive("current", "summary");
            assertThat(ttl).isBetween(Duration.ofSeconds(30), Duration.ofSeconds(40));
        }
    }
}
