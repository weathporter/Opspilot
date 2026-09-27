package com.opspilot.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 运行看板由 Ledger 的事实快照生成，Redis 缓存故障不能伪造空业务数据。 */
class OperationsSummaryServiceTest {
    @Test
    void combinesLedgerSnapshotWithDeploymentMetadata() throws Exception {
        LedgerStatisticsGateway ledger = mock(LedgerStatisticsGateway.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(ledger.snapshot()).thenReturn("{\"accountCount\":2,\"totalBalance\":100,\"trend\":[]}");
        OperationsSummaryService service = new OperationsSummaryService(
                ledger, redis, new ObjectMapper(), "lab", "sha-123");

        var result = service.summary();

        assertEquals(2, result.get("accountCount").asInt());
        assertEquals("lab", result.get("environment").asText());
        assertEquals("sha-123", result.get("version").asText());
        verify(values).set(eq("northledger:operations:summary:v1"), any(), eq(Duration.ofSeconds(30)));
    }

    @Test
    void redisFailureStillReadsLedgerInsteadOfReturningFakeSuccess() throws Exception {
        LedgerStatisticsGateway ledger = mock(LedgerStatisticsGateway.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(ledger.snapshot()).thenReturn("{\"accountCount\":7}");
        OperationsSummaryService service = new OperationsSummaryService(
                ledger, redis, new ObjectMapper(), "lab", "sha-123");

        assertEquals(7, service.summary().get("accountCount").asInt());
        verify(ledger).snapshot();
    }
}
