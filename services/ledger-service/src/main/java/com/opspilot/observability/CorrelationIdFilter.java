package com.opspilot.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 为每个 HTTP 请求建立统一的关联 ID（也称 request ID / trace ID）。
 *
 * <p>入口请求可能已经由网关携带 X-Request-ID；合法时继续沿用，缺失或不安全时生成 UUID。
 * 该值同时写入响应头和日志 MDC，用户提供响应头即可在 Loki 中定位本次请求的全部日志。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** 服务间约定的请求关联头名称。 */
    public static final String HEADER_NAME = "X-Request-ID";

    /** logback 日志格式读取的 MDC 键。 */
    private static final String MDC_KEY = "traceId";

    /**
     * 仅接受 1~64 位字母、数字和有限安全符号。
     * 拒绝空格、换行等字符，防止恶意请求伪造多行日志（日志注入）。
     */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    /**
     * OncePerRequestFilter 保证一次请求分派只执行一次本逻辑。
     * 关联 ID 必须在最前面写入 MDC，所以过滤器优先级设为最高。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            // 放行到后续 Filter、Controller 和业务服务；期间产生的日志都能读取当前 MDC。
            filterChain.doFilter(request, response);
        } finally {
            // Tomcat 会复用线程。无论下游成功或异常都必须清理，避免下一个请求继承错误 traceId。
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 校验上游关联 ID；不可信输入不直接写日志，而是用本服务生成的 UUID 替代。
     *
     * @param candidate 请求头中的候选值，可能为 null
     * @return 可安全用于响应头和日志的关联 ID
     */
    private String resolveCorrelationId(String candidate) {
        if (candidate != null && SAFE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
