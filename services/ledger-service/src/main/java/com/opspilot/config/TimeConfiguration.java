package com.opspilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 与时间有关的基础配置。
 * 把系统时间包装成 Spring Bean 后，业务服务依赖的是可替换的 Clock，测试可以注入固定时间，
 * 避免“测试恰好跨秒/跨日”造成偶发失败。
 */
@Configuration
public class TimeConfiguration {

    /**
     * 提供生产环境的系统时钟。
     * 当前沿用主机默认时区，因此 Linux、Docker 和 JVM 时区必须保持一致；后续企业化可统一为 UTC。
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
