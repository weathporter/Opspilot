package com.opspilot.ledger;

import com.opspilot.account.AccountRepository;
import com.opspilot.transfer.TransferOrderRepository;
import com.opspilot.transfer.TransferStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Operations 只读这个快照，不允许直接查询 Ledger 所拥有的账户与订单表。 */
class LedgerStatisticsServiceTest {
    @Test
    void emptyLedgerProducesCompleteTwentyFourHourTimeline() {
        AccountRepository accounts = mock(AccountRepository.class);
        TransferOrderRepository transfers = mock(TransferOrderRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T07:00:00Z"), ZoneId.of("Asia/Shanghai"));
        when(accounts.sumBalance()).thenReturn(BigDecimal.ZERO);
        when(transfers.sumAmountByStatus(TransferStatus.COMPLETED)).thenReturn(BigDecimal.ZERO);
        when(transfers.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 6))).thenReturn(List.of());
        when(transfers.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                java.time.LocalDateTime.of(2026, 9, 23, 16, 0))).thenReturn(List.of());

        LedgerStatisticsResponse snapshot = new LedgerStatisticsService(accounts, transfers, clock).snapshot();

        assertEquals(0, snapshot.accountCount());
        assertEquals(100.0, snapshot.successRate());
        assertEquals(24, snapshot.trend().size());
        assertEquals(BigDecimal.ZERO, snapshot.totalBalance());
        assertEquals(List.of(), snapshot.recentTransfers());
    }
}
