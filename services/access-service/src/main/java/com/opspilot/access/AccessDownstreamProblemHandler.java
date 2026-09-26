package com.opspilot.access;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/** 下游连接失败时给浏览器返回可追踪的 503，避免把网络故障显示成空数据。 */
@RestControllerAdvice
public class AccessDownstreamProblemHandler {
    @ExceptionHandler(DownstreamUnavailableException.class)
    ProblemDetail unavailable() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "内部服务暂不可用，请稍后重试");
        detail.setProperty("code", "DOWNSTREAM_UNAVAILABLE");
        detail.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
        return detail;
    }
}
