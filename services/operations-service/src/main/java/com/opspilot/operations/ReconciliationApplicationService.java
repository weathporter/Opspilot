package com.opspilot.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对最近有限批次做一致性检查：Ledger 负责提供订单/流水和单笔平衡判断，Operations 负责
 * 保存本次检查的范围和差异线索。没有资金写接口，也不参与 Ledger 的本地转账事务。
 */
@Service
public class ReconciliationApplicationService {
    private final LedgerStatisticsGateway ledger;
    private final ReconciliationPersistenceService persistence;
    private final ObjectMapper json;

    public ReconciliationApplicationService(LedgerStatisticsGateway ledger,
                                            ReconciliationPersistenceService persistence, ObjectMapper json) {
        this.ledger = ledger;
        this.persistence = persistence;
        this.json = json;
    }

    public ReconciliationRunResponse run(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("对账批次大小必须在 1 至 100 之间");
        }
        String response = ledger.reconciliationCandidates(limit);
        try {
            JsonNode candidates = json.readTree(response);
            if (candidates == null || !candidates.isArray()) {
                throw new DownstreamUnavailableException("Ledger returned a non-array reconciliation response", null);
            }
            List<String> discrepancies = new ArrayList<>();
            for (JsonNode candidate : candidates) {
                JsonNode requestId = candidate.path("transfer").path("requestId");
                JsonNode balanced = candidate.path("balanced");
                if (!requestId.isTextual() || requestId.asText().isBlank() || !balanced.isBoolean()) {
                    throw new DownstreamUnavailableException("Ledger returned an incomplete reconciliation item", null);
                }
                if (!balanced.asBoolean()) {
                    discrepancies.add(requestId.asText());
                }
            }
            // 远程请求和 JSON 检查结束后才进入独立的数据库事务。
            return persistence.persist(candidates.size(), discrepancies);
        } catch (JsonProcessingException exception) {
            throw new DownstreamUnavailableException("Ledger returned invalid reconciliation JSON", exception);
        }
    }
}
