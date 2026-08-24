package com.opspilot.dashboard;

import com.opspilot.NorthLedgerIntegrationTest;
import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountRepository;
import com.opspilot.account.CreateAccountRequest;
import com.opspilot.common.DuplicateResourceException;
import com.opspilot.transfer.CreateTransferRequest;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferApplicationService;
import com.opspilot.transfer.TransferOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 MySQL 写事务与 Redis 读模型之间的一致性边界。
 *
 * <p>缓存失效必须发生在提交成功之后：成功开户/转账要让下一次总览回源重建，失败事务不能
 * 清掉仍然正确的旧快照。测试走真实事务、仓储和 Redis，不把事件监听器替换成 mock。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class OperationsSummaryCacheInvalidationIntegrationTest extends NorthLedgerIntegrationTest {

    private static final String SUMMARY_CACHE_KEY =
            "northledger:cache:operations-summary::current";

    @Autowired
    private AccountApplicationService accountService;

    @Autowired
    private TransferApplicationService transferService;

    @Autowired
    private OperationsDashboardApplicationService dashboardService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanBusinessFactsAndSummaryCache() {
        // 按外键依赖逆序清理业务表；Redis 只删除本测试拥有的总览键，不影响会话命名空间。
        ledgerEntryRepository.deleteAll();
        transferOrderRepository.deleteAll();
        accountRepository.deleteAll();
        redisTemplate.delete(SUMMARY_CACHE_KEY);
    }

    @Test
    void committedAccountCreationEvictsCachedSummary() {
        OperationsSummaryResponse before = dashboardService.getSummary();
        assertThat(before.accountCount()).isZero();

        accountService.create(account("6222000000003101", "新增账户", "100.00"));
        OperationsSummaryResponse after = dashboardService.getSummary();

        assertThat(after.accountCount()).isOne();
        assertThat(after.generatedAt())
                .as("提交成功后必须删除旧快照并从 MySQL 生成新快照")
                .isNotEqualTo(before.generatedAt());
    }

    @Test
    void rolledBackAccountCreationDoesNotEvictStillValidSnapshot() {
        CreateAccountRequest request = account("6222000000003201", "唯一账户", "100.00");
        accountService.create(request);
        OperationsSummaryResponse beforeFailure = dashboardService.getSummary();

        assertThatThrownBy(() -> accountService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        OperationsSummaryResponse afterFailure = dashboardService.getSummary();
        assertThat(afterFailure.generatedAt())
                .as("唯一约束失败并回滚时，提交后监听器不能运行")
                .isEqualTo(beforeFailure.generatedAt());
        assertThat(afterFailure.accountCount()).isOne();
    }

    @Test
    void committedTransferEvictsCachedSummary() {
        accountService.create(account("6222000000003301", "付款账户", "1000.00"));
        accountService.create(account("6222000000003302", "收款账户", "100.00"));
        OperationsSummaryResponse before = dashboardService.getSummary();

        transferService.transfer(
                "cache-invalidation-transfer-001",
                new CreateTransferRequest(
                        "6222000000003301",
                        "6222000000003302",
                        new BigDecimal("250.00")
                )
        );
        OperationsSummaryResponse after = dashboardService.getSummary();

        assertThat(after.completedTransferCount()).isOne();
        assertThat(after.completedTransferVolume()).isEqualByComparingTo("250.00");
        assertThat(after.generatedAt()).isNotEqualTo(before.generatedAt());
    }

    private static CreateAccountRequest account(
            String accountNo,
            String holderName,
            String openingBalance
    ) {
        return new CreateAccountRequest(accountNo, holderName, new BigDecimal(openingBalance));
    }
}
