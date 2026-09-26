package com.opspilot.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单条不可变资金流水，映射 {@code ledger_entry} 表。
 * 账户表保存当前余额，流水表保存余额如何变化的证据，两者共同支持审计和故障追溯。
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    /** 流水内部主键，只用于持久化排序和关联。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属订单采用 LAZY 加载，读取流水时不会无条件加载完整订单对象。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_order_id", nullable = false)
    private TransferOrder transferOrder;

    /** 发生余额变化的业务账号。 */
    @Column(name = "account_no", nullable = false, length = 32)
    private String accountNo;

    /** DEBIT 表示扣款，CREDIT 表示入账；金额本身始终为正数。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 8)
    private LedgerEntryType entryType;

    /** 本次发生额，精度与账户余额一致。 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** 交易后的余额快照，可结合方向反推出交易前余额。 */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** 与订单和余额更新共享同一业务时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 仅供 JPA 反射使用，业务代码必须通过工厂方法创建合法流水。 */
    protected LedgerEntry() {
    }

    private LedgerEntry(
            TransferOrder transferOrder,
            String accountNo,
            LedgerEntryType entryType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime createdAt
    ) {
        this.transferOrder = transferOrder;
        this.accountNo = accountNo;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    /** 创建付款方借记流水。 */
    public static LedgerEntry debit(
            TransferOrder transferOrder,
            String accountNo,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime now
    ) {
        return new LedgerEntry(transferOrder, accountNo, LedgerEntryType.DEBIT, amount, balanceAfter, now);
    }

    /** 创建收款方贷记流水。 */
    public static LedgerEntry credit(
            TransferOrder transferOrder,
            String accountNo,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime now
    ) {
        return new LedgerEntry(transferOrder, accountNo, LedgerEntryType.CREDIT, amount, balanceAfter, now);
    }

    /** @return 数据库内部流水主键 */
    public Long getId() {
        return id;
    }

    /** 只读对账批次需要按订单归组；写入与修改仍只能通过转账事务。 */
    public TransferOrder getTransferOrder() {
        return transferOrder;
    }

    /** @return 发生资金变化的账号 */
    public String getAccountNo() {
        return accountNo;
    }

    /** @return 借记或贷记方向 */
    public LedgerEntryType getEntryType() {
        return entryType;
    }

    /** @return 本次发生额 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return 交易后余额 */
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    /** @return 流水创建时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
