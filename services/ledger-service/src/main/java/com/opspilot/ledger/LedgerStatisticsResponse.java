package com.opspilot.ledger;

import com.opspilot.transfer.TransferResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ledger 拥有的资金事实只读快照；Operations 可以展示、缓存，但不能改写其统计口径。 */
public record LedgerStatisticsResponse(
        LocalDateTime generatedAt,
        long accountCount,
        BigDecimal totalBalance,
        long transferCount,
        long completedTransferCount,
        BigDecimal completedTransferVolume,
        double successRate,
        List<HourlyTransferTrend> trend,
        List<TransferResponse> recentTransfers
) {
}
