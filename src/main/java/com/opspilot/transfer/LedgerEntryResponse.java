package com.opspilot.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 前端可直接展示的单条流水证据。
 *
 * @param entryId 内部流水号，仅用于本地演示和排序
 * @param accountNo 发生变化的业务账号
 * @param entryType 借记或贷记方向
 * @param amount 本次发生额
 * @param balanceBefore 由方向、发生额和交易后余额反推出的交易前余额
 * @param balanceAfter 数据库持久化的交易后余额快照
 * @param createdAt 流水创建时间
 */
public record LedgerEntryResponse(
        Long entryId,
        String accountNo,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {
    /** 把实体转换为脱离持久化上下文的只读快照。 */
    public static LedgerEntryResponse from(LedgerEntry entry) {
        BigDecimal balanceBefore = entry.getEntryType() == LedgerEntryType.DEBIT
                ? entry.getBalanceAfter().add(entry.getAmount())
                : entry.getBalanceAfter().subtract(entry.getAmount());
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getAccountNo(),
                entry.getEntryType(),
                entry.getAmount(),
                balanceBefore,
                entry.getBalanceAfter(),
                entry.getCreatedAt()
        );
    }
}
