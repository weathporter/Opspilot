package com.opspilot.ledger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 对账务服务的内部 HTTP 入口验证服务凭据。
 *
 * <p>NetworkPolicy 负责减少能连到本 Pod 的来源，但网络策略不是身份认证；即便有人在集群
 * 内误配了访问规则，缺少 Secret 中的凭据也不能调用资金接口。浏览器 Cookie 和用户自填的
 * 身份请求头在这里都不构成认证依据。</p>
 */
public final class InternalServiceCredentialFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PREFIX = "/internal/v1/ledger/";
    private static final String BEARER_PREFIX = "Bearer ";
    private final byte[] accessCredential;
    private final byte[] operationsReadCredential;

    /** 启动时拒绝空凭据，避免环境变量漏配后把所有内部端点意外开放。 */
    public InternalServiceCredentialFilter(String accessToken, String operationsReadToken) {
        if (accessToken == null || accessToken.isBlank()
                || operationsReadToken == null || operationsReadToken.isBlank()
                || accessToken.equals(operationsReadToken)) {
            throw new IllegalArgumentException("Distinct internal read and write tokens are required");
        }
        this.accessCredential = accessToken.getBytes(StandardCharsets.UTF_8);
        this.operationsReadCredential = operationsReadToken.getBytes(StandardCharsets.UTF_8);
    }

    /** 探针和非内部路径交给 Spring Security 的独立授权规则处理。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            reject(response);
            return;
        }
        byte[] presented = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        boolean isAccess = MessageDigest.isEqual(accessCredential, presented);
        boolean isOperationsRead = MessageDigest.isEqual(operationsReadCredential, presented);
        if (!isAccess && !isOperationsRead) {
            reject(response);
            return;
        }

        // 读令牌只能访问统计端点；Access 令牌才能调用账户/转账接口。
        String principal = isAccess ? "access-service" : "operations-service";
        String authority = isAccess ? "ROLE_ACCESS" : "ROLE_OPERATIONS_READ";
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 同一容器线程会处理后续请求，必须清理 ThreadLocal，避免身份串到下一请求。
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        // 不回显请求头或凭据；日志也只记录拒绝事件，不保存 Secret 内容。
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"code\":\"INVALID_SERVICE_CREDENTIAL\"}");
    }
}
