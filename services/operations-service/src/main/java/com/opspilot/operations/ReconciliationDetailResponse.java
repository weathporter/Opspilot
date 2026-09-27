package com.opspilot.operations;

import java.time.LocalDateTime;
import java.util.List;

/** 故障追查需要的持久化证据：批次时间、抽样范围与不平衡订单键。 */
public record ReconciliationDetailResponse(long id, LocalDateTime completedAt, int checkedCount,
                                           int discrepancyCount, String status,
                                           List<String> discrepancyRequestIds) {
}
