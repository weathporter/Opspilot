package com.opspilot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Spring Security 官方 SPA 模式的 CSRF 适配器。
 *
 * <p>服务端渲染属性继续使用 XOR 编码以抵御 BREACH；React 从 Cookie 读取原始 Token 并
 * 放入请求头时，则交给普通处理器解析。二者同时存在才能兼容延迟 Token 与单页应用。</p>
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html">Spring Security CSRF</a>
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    /** 使用 XOR 处理请求属性，并强制解析延迟 Token 以确保 Cookie 被写回。 */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken
    ) {
        xor.handle(request, response, csrfToken);
        csrfToken.get().getToken();
    }

    /** 请求头存在时解析 Cookie 原始值，否则保留 Spring 的 XOR 参数兼容路径。 */
    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (headerValue != null ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
