package com.opspilot.common;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API 的统一异常出口。
 *
 * <p>业务代码只需要抛出有语义的异常，本类负责把它翻译为稳定的 HTTP 状态码和
 * RFC 7807/9457 风格 ProblemDetail。这样 Controller 不必重复 try/catch，调用方也不会
 * 收到框架堆栈或格式各异的错误响应。</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 资源不存在属于查询结果缺失，映射为 404 Not Found。 */
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage());
    }

    /** 唯一键或幂等键冲突表示当前请求与已有资源冲突，映射为 409 Conflict。 */
    @ExceptionHandler(DuplicateResourceException.class)
    ProblemDetail handleConflict(DuplicateResourceException exception) {
        return problem(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage());
    }

    /** 请求格式合法但违反业务规则（如余额不足），映射为 422。 */
    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(), exception.getMessage());
    }

    /**
     * 汇总 Bean Validation 的字段错误。
     * LinkedHashMap 保留校验器返回顺序；putIfAbsent 避免同一字段多条规则失败时响应过度膨胀。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数校验失败");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    /**
     * 构造所有错误共用的响应骨架。
     *
     * <p>code 供程序稳定判断；detail 供人阅读；timestamp 便于按时间查日志；traceId 则把
     * 客户端报错与服务端同一请求的日志关联起来。不要把异常堆栈直接返回给外部调用方。</p>
     */
    private ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setType(URI.create("https://opspilot.local/problems/" + code.toLowerCase()));
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now());

        // CorrelationIdFilter 已把请求 ID 放入 MDC；非 HTTP 场景没有该值，因此先判空。
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
        return detail;
    }
}
