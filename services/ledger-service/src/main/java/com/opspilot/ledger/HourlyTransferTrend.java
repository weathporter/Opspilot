package com.opspilot.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 按整点聚合的只读交易量，字段名保持与现有前端图表一致。 */
public record HourlyTransferTrend(LocalDateTime hour, long completedCount,
                                  long processingCount, BigDecimal completedAmount) {
}
