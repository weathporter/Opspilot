package com.opspilot.ledger;

import com.opspilot.transfer.LedgerEntry;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferAuditResponse;
import com.opspilot.transfer.TransferOrder;
import com.opspilot.transfer.TransferOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ledger 给 Operations 的有限只读对账材料；不把数据库连接或实体修改权跨服务共享。 */
@Service
public class LedgerReconciliationService {
    private final TransferOrderRepository orders;
    private final LedgerEntryRepository entries;

    public LedgerReconciliationService(TransferOrderRepository orders, LedgerEntryRepository entries) {
        this.orders = orders;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public List<TransferAuditResponse> candidates(int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        List<TransferOrder> batch = orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0, bounded));
        if (batch.isEmpty()) {
            return List.of();
        }
        List<String> ids = batch.stream().map(TransferOrder::getRequestId).toList();
        Map<String, List<LedgerEntry>> byRequest = entries.findAllForRequestIds(ids).stream()
                .collect(Collectors.groupingBy(entry -> entry.getTransferOrder().getRequestId()));
        return batch.stream().map(order -> TransferAuditResponse.of(order,
                byRequest.getOrDefault(order.getRequestId(), List.of()))).toList();
    }
}
