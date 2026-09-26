package com.opspilot.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 运维服务只缓存可重建的 Ledger 快照，不缓存资金写入结果，也不复制 Ledger 的数据库表。
 * 缓存不可用时仍请求 Ledger；Ledger 不可用时抛 503，绝不返回看似正常的零值看板。
 */
@Service
public class OperationsSummaryService {
    private static final String CACHE_KEY = "northledger:operations:summary:v1";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private final LedgerStatisticsGateway ledger;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final String environment;
    private final String version;

    public OperationsSummaryService(LedgerStatisticsGateway ledger, StringRedisTemplate redis, ObjectMapper json,
                                    @Value("${info.app.environment:local}") String environment,
                                    @Value("${info.app.version:dev}") String version) {
        this.ledger = ledger;
        this.redis = redis;
        this.json = json;
        this.environment = environment;
        this.version = version;
    }

    public JsonNode summary() {
        String snapshot = null;
        try {
            snapshot = redis.opsForValue().get(CACHE_KEY);
        } catch (RuntimeException ignored) {
            // Redis 是可丢弃的性能层；故障时改走 Ledger，不把缓存异常伪装为资金服务故障。
        }
        if (snapshot == null || !validSnapshot(snapshot)) {
            snapshot = ledger.snapshot();
            if (!validSnapshot(snapshot)) {
                throw new DownstreamUnavailableException("Ledger returned an invalid statistics snapshot", null);
            }
            try {
                redis.opsForValue().set(CACHE_KEY, snapshot, CACHE_TTL);
            } catch (RuntimeException ignored) {
                // 写缓存失败不影响已经成功读取的事实数据；下一请求可重新尝试。
            }
        }
        try {
            ObjectNode result = (ObjectNode) json.readTree(snapshot);
            // 部署元数据属于 Operations 运行实例；不写入共享缓存，滚动发布时不会混淆版本。
            result.put("environment", environment);
            result.put("version", version);
            return result;
        } catch (JsonProcessingException exception) {
            throw new DownstreamUnavailableException("Ledger statistics JSON is invalid", exception);
        }
    }

    private boolean validSnapshot(String snapshot) {
        if (snapshot == null) {
            return false;
        }
        try {
            JsonNode parsed = json.readTree(snapshot);
            return parsed != null && parsed.isObject() && parsed.has("accountCount");
        } catch (JsonProcessingException exception) {
            return false;
        }
    }
}
