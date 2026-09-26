package com.opspilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * 安全过滤器专用的 RFC ProblemDetail 写出器。
 *
 * <p>认证与授权失败发生在 Controller 之前，无法进入 {@code ApiExceptionHandler}，因此
 * 这里复用同一字段约定，避免前端面对两种互不兼容的错误结构。</p>
 */
@Component
public class SecurityProblemWriter {

    private final ObjectMapper objectMapper;

    public SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 写出不含堆栈、内部类名或敏感认证细节的 JSON 错误。 */
    public void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setType(URI.create("https://northledger.local/problems/" + code.toLowerCase()));
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
