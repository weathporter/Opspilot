package com.opspilot.transfer;

import com.opspilot.MySqlIntegrationTest;
import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountRepository;
import com.opspilot.account.CreateAccountRequest;
import com.opspilot.common.BusinessRuleException;
import com.opspilot.common.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 转账事务、幂等和流水一致性的 MySQL 集成测试。
 *
 * <p>这些测试刻意穿过 ApplicationService、JPA、Flyway 表结构和真实 MySQL，
 * 因为单元测试无法证明数据库事务回滚、唯一索引和 decimal 行为完全符合预期。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferFlowIntegrationTest extends MySqlIntegrationTest {

    /** 用于准备账户并读取转账后的余额。 */
    @Autowired
    private AccountApplicationService accountApplicationService;

    /** 被测的转账核心用例。 */
    @Autowired
    private TransferApplicationService transferApplicationService;

    /** 以下三个仓储用于清理和直接断言数据库记录数量。 */
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    /**
     * 每个测试前按外键依赖的反方向清理数据：先流水，再订单，最后账户。
     * 顺序错误会触发 MySQL 外键约束，正好也说明删除/迁移操作必须理解表关系。
     */
    @BeforeEach
    void cleanDatabase() {
        ledgerEntryRepository.deleteAll();
        transferOrderRepository.deleteAll();
        accountRepository.deleteAll();
    }

    /**
     * 证明相同幂等键和相同参数重试时只转账一次。
     * 除响应相同外，还同时断言余额、订单数和双边流水数，避免只验证表面返回值。
     */
    @Test
    void transfersMoneyOnlyOnceWhenIdempotencyKeyIsRetried() {
        createAccount("6222000000000001", "张三", "1000.00");
        createAccount("6222000000000002", "李四", "100.00");
        CreateTransferRequest request = new CreateTransferRequest(
                "6222000000000001",
                "6222000000000002",
                new BigDecimal("250.00")
        );

        TransferResponse first = transferApplicationService.transfer("transfer-demo-001", request);
        TransferResponse replay = transferApplicationService.transfer("transfer-demo-001", request);

        assertThat(first.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(replay).isEqualTo(first);
        assertThat(accountApplicationService.get("6222000000000001").balance())
                .isEqualByComparingTo("750.00");
        assertThat(accountApplicationService.get("6222000000000002").balance())
                .isEqualByComparingTo("350.00");
        assertThat(transferOrderRepository.count()).isEqualTo(1);
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
    }

    /**
     * 证明余额不足时整个事务回滚。
     * 两个余额保持原值且订单/流水均为零，说明没有留下“部分成功”的脏数据。
     */
    @Test
    void rollsBackTheWholeTransferWhenBalanceIsInsufficient() {
        createAccount("6222000000000011", "王五", "50.00");
        createAccount("6222000000000012", "赵六", "100.00");
        CreateTransferRequest request = new CreateTransferRequest(
                "6222000000000011",
                "6222000000000012",
                new BigDecimal("80.00")
        );

        assertThatThrownBy(() -> transferApplicationService.transfer("transfer-demo-002", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("账户余额不足");

        assertThat(accountApplicationService.get("6222000000000011").balance())
                .isEqualByComparingTo("50.00");
        assertThat(accountApplicationService.get("6222000000000012").balance())
                .isEqualByComparingTo("100.00");
        assertThat(transferOrderRepository.count()).isZero();
        assertThat(ledgerEntryRepository.count()).isZero();
    }

    /**
     * 证明幂等键不能代表两笔不同交易。
     * 如果只按键返回旧结果而不比较载荷，调用方误用键时会得到极难发现的错误结果。
     */
    @Test
    void rejectsReusingAnIdempotencyKeyForDifferentPayload() {
        createAccount("6222000000000021", "甲", "500.00");
        createAccount("6222000000000022", "乙", "0.00");
        createAccount("6222000000000023", "丙", "0.00");
        transferApplicationService.transfer(
                "transfer-demo-003",
                new CreateTransferRequest(
                        "6222000000000021",
                        "6222000000000022",
                        new BigDecimal("10.00")
                )
        );

        assertThatThrownBy(() -> transferApplicationService.transfer(
                "transfer-demo-003",
                new CreateTransferRequest(
                        "6222000000000021",
                        "6222000000000023",
                        new BigDecimal("10.00")
                )
        ))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("相同幂等键不能用于不同的转账请求");
    }

    /** 使用字符串构造 BigDecimal，避免 double 字面量引入精度误差。 */
    private void createAccount(String accountNo, String holderName, String openingBalance) {
        accountApplicationService.create(new CreateAccountRequest(
                accountNo,
                holderName,
                new BigDecimal(openingBalance)
        ));
    }
}
