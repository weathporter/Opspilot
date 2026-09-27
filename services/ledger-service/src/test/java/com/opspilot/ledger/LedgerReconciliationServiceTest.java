package com.opspilot.ledger;

import com.opspilot.transfer.LedgerEntry;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferOrder;
import com.opspilot.transfer.TransferOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 对账输入是 Ledger 的受限只读快照，Operations 不跨库直接查询资金表。 */
class LedgerReconciliationServiceTest {
    @Test
    void returnsOrderAndBothEntriesForBoundedBatch() {
        TransferOrderRepository orders = mock(TransferOrderRepository.class);
        LedgerEntryRepository entries = mock(LedgerEntryRepository.class);
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        TransferOrder order = TransferOrder.start("key-1", "12345678", "87654321",
                new BigDecimal("25.00"), now);
        order.complete(now);
        when(orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))).thenReturn(List.of(order));
        when(entries.findAllForRequestIds(List.of("key-1"))).thenReturn(List.of(
                LedgerEntry.debit(order, "12345678", new BigDecimal("25.00"),
                        new BigDecimal("75.00"), now),
                LedgerEntry.credit(order, "87654321", new BigDecimal("25.00"),
                        new BigDecimal("25.00"), now)));

        var result = new LedgerReconciliationService(orders, entries).candidates(5);

        assertTrue(result.get(0).balanced());
    }
}
