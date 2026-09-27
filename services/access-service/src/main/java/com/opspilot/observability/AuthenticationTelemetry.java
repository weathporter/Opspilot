package com.opspilot.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 认证结果的低基数 Prometheus 指标。
 *
 * <p>outcome 只有 success/failure 两个固定值；用户名、IP 和 traceId 只进入审计/日志，
 * 绝不作为指标标签，否则用户增长会制造不可控的时间序列数量。</p>
 */
@Component
public class AuthenticationTelemetry {

    private final Counter success;
    private final Counter failure;

    public AuthenticationTelemetry(MeterRegistry meterRegistry) {
        this.success = Counter.builder("northledger.authentication.attempts")
                .description("NorthLedger username/password authentication attempts")
                .tag("outcome", "success")
                .register(meterRegistry);
        this.failure = Counter.builder("northledger.authentication.attempts")
                .description("NorthLedger username/password authentication attempts")
                .tag("outcome", "failure")
                .register(meterRegistry);
    }

    public void recordSuccess() {
        success.increment();
    }

    public void recordFailure() {
        failure.increment();
    }
}
