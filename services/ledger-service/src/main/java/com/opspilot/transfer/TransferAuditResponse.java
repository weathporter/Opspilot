package com.opspilot.transfer;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一笔转账的审计闭环：订单结果、两条资金流水和自动平衡校验。
 *
 * @param transfer 转账订单快照
 * @param entries 借记、贷记流水
 * @param balanced 两条流水方向相反且金额相等时为 true
 */
public record TransferAuditResponse(
        TransferResponse transfer,
        List<LedgerEntryResponse> entries,
        boolean balanced
) {
    /** 根据流水集合计算双录是否平衡，避免由前端自行猜测正确性。 */
    public static TransferAuditResponse of(TransferOrder order, List<LedgerEntry> ledgerEntries) {
        List<LedgerEntryResponse> entries = ledgerEntries.stream()
                .map(LedgerEntryResponse::from)
                .toList();

        BigDecimal debit = ledgerEntries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = ledgerEntries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 仅借贷相等不足以证明交易正确：两笔 50 元流水不能对应 100 元订单。
        // 还要确认方向、账号与订单本身一致，避免交叉引用错误被看板误报为平衡。
        boolean balanced = ledgerEntries.size() == 2
                && debit.compareTo(order.getAmount()) == 0
                && credit.compareTo(order.getAmount()) == 0
                && ledgerEntries.stream().anyMatch(entry -> entry.getEntryType() == LedgerEntryType.DEBIT
                        && entry.getAccountNo().equals(order.getSourceAccountNo()))
                && ledgerEntries.stream().anyMatch(entry -> entry.getEntryType() == LedgerEntryType.CREDIT
                        && entry.getAccountNo().equals(order.getTargetAccountNo()));
        return new TransferAuditResponse(TransferResponse.from(order), entries, balanced);
    }
}
