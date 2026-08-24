package com.opspilot.dashboard;

import com.opspilot.NorthLedgerIntegrationTest;
import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountRepository;
import com.opspilot.account.CreateAccountRequest;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 用真实 Redis 进程中断证明业务缓存可以降级，而不是只对异常处理器做单元测试。
 *
 * <p>该测试直接调用应用服务，刻意绕开 HTTP 会话过滤器：Redis 同时承载 Session，真实 HTTP
 * 请求可能先因会话不可读而失败；这里验证的承诺仅是“缓存层故障不会阻断 MySQL 总览查询”。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class OperationsSummaryCacheDegradationIntegrationTest extends NorthLedgerIntegrationTest {

    private static final String SUMMARY_CACHE_KEY =
            "northledger:cache:operations-summary::current";

    @Autowired
    private OperationsDashboardApplicationService dashboardService;

    @Autowired
    private AccountApplicationService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void prepareSingleAccount() {
        ledgerEntryRepository.deleteAll();
        transferOrderRepository.deleteAll();
        accountRepository.deleteAll();
        redisTemplate.delete(SUMMARY_CACHE_KEY);
        accountService.create(new CreateAccountRequest(
                "6222000000004101",
                "缓存降级账户",
                new BigDecimal("100.00")
        ));
    }

    @Test
    void redisOutageFallsBackToMySqlAndEmitsGetErrorMetric() {
        OperationsSummaryResponse cached = dashboardService.getSummary();
        assertThat(redisTemplate.hasKey(SUMMARY_CACHE_KEY)).isTrue();

        double errorsBefore = errorCount("get");
        // 停止本测试类拥有的临时 Redis，模拟进程不可达；1 秒命令超时限制了降级等待时间。
        REDIS.stop();

        assertThatCode(() -> {
            OperationsSummaryResponse fallback = dashboardService.getSummary();
            assertThat(fallback.accountCount()).isOne();
            assertThat(fallback.totalBalance()).isEqualByComparingTo("100.00");
            assertThat(fallback.generatedAt()).isNotEqualTo(cached.generatedAt());
        }).doesNotThrowAnyException();

        assertThat(errorCount("get"))
                .as("真实 Redis GET 失败必须留下低基数指标")
                .isGreaterThan(errorsBefore);
    }

    private double errorCount(String operation) {
        return meterRegistry.find("northledger.cache.errors")
                .tag("operation", operation)
                .tag("cache", "operations-summary")
                .counter() == null
                ? 0.0
                : meterRegistry.get("northledger.cache.errors")
                        .tag("operation", operation)
                        .tag("cache", "operations-summary")
                        .counter()
                        .count();
    }
}
