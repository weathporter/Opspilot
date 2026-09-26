package com.opspilot.ledger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opspilot.transfer.TransferAuditResponse;
import java.util.List;

/** 仅 Operations 的只读服务凭据和 Access 的内部凭据可读取；不经公网入口暴露。 */
@RestController
@RequestMapping("/internal/v1/ledger/statistics")
public class LedgerStatisticsController {
    private final LedgerStatisticsService statistics;
    private final LedgerReconciliationService reconciliation;

    public LedgerStatisticsController(LedgerStatisticsService statistics,
                                      LedgerReconciliationService reconciliation) {
        this.statistics = statistics;
        this.reconciliation = reconciliation;
    }

    @GetMapping("/snapshot")
    public LedgerStatisticsResponse snapshot() {
        return statistics.snapshot();
    }

    /** 对账材料上限 100 笔；新记录和两条流水在同一个只读事务中获取。 */
    @GetMapping("/reconciliation-candidates")
    public List<TransferAuditResponse> reconciliationCandidates(@RequestParam(defaultValue = "100") int limit) {
        return reconciliation.candidates(limit);
    }
}
