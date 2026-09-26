package com.opspilot.operations;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/** 下游故障用稳定 503 契约表达，既不返回假看板，也不泄露内部凭据或堆栈。 */
@RestControllerAdvice
public class OperationsProblemHandler {
    @ExceptionHandler(DownstreamUnavailableException.class)
    ProblemDetail unavailable() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "账务统计暂不可用，请稍后重试");
        detail.setProperty("code", "LEDGER_UNAVAILABLE");
        detail.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
        return detail;
    }

    @ExceptionHandler(ReconciliationNotFoundException.class)
    ProblemDetail notFound() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "对账批次不存在");
        detail.setProperty("code", "RECONCILIATION_NOT_FOUND");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "对账请求参数不合法");
        detail.setProperty("code", "INVALID_RECONCILIATION_REQUEST");
        return detail;
    }
}
