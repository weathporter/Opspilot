package com.opspilot.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Operations 判读只读候选集并保存独立的运行历史，不修改 Ledger 的余额或流水。 */
class ReconciliationApplicationServiceTest {
    @Test
    void recordsDiscrepancyWithoutChangingLedger() {
        LedgerStatisticsGateway ledger = mock(LedgerStatisticsGateway.class);
        ReconciliationPersistenceService persistence = mock(ReconciliationPersistenceService.class);
        when(ledger.reconciliationCandidates(20)).thenReturn("["
                + "{\"transfer\":{\"requestId\":\"ok-1\"},\"balanced\":true},"
                + "{\"transfer\":{\"requestId\":\"bad-2\"},\"balanced\":false}]");
        when(persistence.persist(2, List.of("bad-2"))).thenReturn(
                new ReconciliationRunResponse(7L, java.time.LocalDateTime.of(2026, 9, 24, 12, 0),
                        2, 1, "DISCREPANCY"));

        var result = new ReconciliationApplicationService(ledger, persistence, new ObjectMapper()).run(20);

        assertEquals(1, result.discrepancyCount());
        verify(persistence).persist(2, List.of("bad-2"));
    }
}
