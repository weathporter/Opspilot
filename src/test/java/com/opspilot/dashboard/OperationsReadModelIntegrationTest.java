package com.opspilot.dashboard;

import com.opspilot.MySqlIntegrationTest;
import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountRepository;
import com.opspilot.account.CreateAccountRequest;
import com.opspilot.transfer.CreateTransferRequest;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferApplicationService;
import com.opspilot.transfer.TransferAuditResponse;
import com.opspilot.transfer.TransferOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 控制台读模型的真实 MySQL 集成测试。
 *
 * <p>前端页面依赖账户列表、转账列表、双录审计和运行汇总；这些接口若只靠 TypeScript
 * mock 数据验证，无法证明 JPA 查询、聚合口径和实体到 DTO 的转换正确。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class OperationsReadModelIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private AccountApplicationService accountApplicationService;

    @Autowired
    private TransferApplicationService transferApplicationService;

    @Autowired
    private OperationsDashboardApplicationService dashboardApplicationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    /** 按外键依赖反向清理，保证测试结果不受其他用例残留数据影响。 */
    @BeforeEach
    void cleanDatabase() {
        ledgerEntryRepository.deleteAll();
        transferOrderRepository.deleteAll();
        accountRepository.deleteAll();
    }

    /**
     * 走通“开户 → 转账 → 列表 → 双录流水 → 运行总览”，即业务前端实际依赖的完整读写闭环。
     */
    @Test
    void exposesAccountsTransfersLedgerAndDashboardFromRealMySql() {
        accountApplicationService.create(new CreateAccountRequest(
                "6222000000000101",
                "运行资金账户",
                new BigDecimal("1000.00")
        ));
        accountApplicationService.create(new CreateAccountRequest(
                "6222000000000102",
                "清算账户",
                new BigDecimal("100.00")
        ));

        transferApplicationService.transfer(
                "dashboard-demo-001",
                new CreateTransferRequest(
                        "6222000000000101",
                        "6222000000000102",
                        new BigDecimal("250.00")
                )
        );

        assertThat(accountApplicationService.list("运行资金", 10))
                .singleElement()
                .satisfies(account -> assertThat(account.accountNo()).isEqualTo("6222000000000101"));
        assertThat(transferApplicationService.list(null, 10))
                .singleElement()
                .satisfies(transfer -> assertThat(transfer.requestId()).isEqualTo("dashboard-demo-001"));

        TransferAuditResponse audit = transferApplicationService.audit("dashboard-demo-001");
        assertThat(audit.balanced()).isTrue();
        assertThat(audit.entries()).hasSize(2);
        assertThat(audit.entries())
                .extracting(entry -> entry.balanceAfter())
                .containsExactly(new BigDecimal("750.00"), new BigDecimal("350.00"));

        OperationsSummaryResponse summary = dashboardApplicationService.getSummary();
        assertThat(summary.accountCount()).isEqualTo(2);
        assertThat(summary.totalBalance()).isEqualByComparingTo("1100.00");
        assertThat(summary.transferCount()).isEqualTo(1);
        assertThat(summary.completedTransferVolume()).isEqualByComparingTo("250.00");
        assertThat(summary.trend()).hasSize(24);
        assertThat(summary.recentTransfers()).hasSize(1);
    }
}
