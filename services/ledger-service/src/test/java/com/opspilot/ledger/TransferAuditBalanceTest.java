package com.opspilot.ledger;

import com.opspilot.transfer.LedgerEntry;
import com.opspilot.transfer.TransferAuditResponse;
import com.opspilot.transfer.TransferOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** 双边流水相等还不够，必须与订单金额本身一致。 */
class TransferAuditBalanceTest {
    @Test
    void equalEntriesWithWrongOrderAmountAreNotBalanced() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        TransferOrder order = TransferOrder.start("id-1", "12345678", "87654321",
                new BigDecimal("100.00"), now);
        var entries = List.of(
                LedgerEntry.debit(order, "12345678", new BigDecimal("50.00"),
                        new BigDecimal("950.00"), now),
                LedgerEntry.credit(order, "87654321", new BigDecimal("50.00"),
                        new BigDecimal("150.00"), now));

        assertFalse(TransferAuditResponse.of(order, entries).balanced());
    }
}
