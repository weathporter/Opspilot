package com.opspilot.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运行总览中的单个小时聚合点。
 * 前端只负责画图，不再自行合并原始订单，从而保证所有展示端使用同一统计口径。
 */
public record TransferTrendPoint(
        LocalDateTime hour,
        long completedCount,
        long processingCount,
        BigDecimal completedAmount
) {
}
