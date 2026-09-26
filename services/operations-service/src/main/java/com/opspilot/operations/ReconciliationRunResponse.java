package com.opspilot.operations;

import java.time.LocalDateTime;

/** 一次对账批次的概要；差异明细通过单独查询接口读取。 */
public record ReconciliationRunResponse(long id, LocalDateTime completedAt,
                                        int checkedCount, int discrepancyCount, String status) {
}
