package com.opspilot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 浏览器会话的查询与注销入口；登录 POST 由 Spring Security 过滤器处理。 */
@RestController
@RequestMapping("/api/v1/auth/session")
public class AuthSessionController {

    private final AuthSessionService authSessionService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SecurityAuditRecorder securityAuditRecorder;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthSessionController(
            AuthSessionService authSessionService,
            CsrfTokenRepository csrfTokenRepository,
            SecurityAuditRecorder securityAuditRecorder
    ) {
        this.authSessionService = authSessionService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.securityAuditRecorder = securityAuditRecorder;
    }

    /**
     * 返回当前登录状态。读取 {@code csrfToken.getToken()} 会触发延迟 Token 真正生成并写入
     * XSRF-TOKEN Cookie，前端之后才能对 POST/DELETE 请求设置对应请求头。
     */
    @GetMapping
    public AuthSessionResponse current(
            Authentication authentication,
            CsrfToken csrfToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 登录和注销会清掉旧 Token；显式保存让本端点成为前端统一、可测试的 Token 刷新入口。
        csrfTokenRepository.saveToken(csrfToken, request, response);
        return authSessionService.from(authentication);
    }

    /**
     * 删除服务端会话并清空 SecurityContext。DELETE 本身仍受 CSRF 保护，恶意站点无法让
     * 用户在不知情时注销或借同一路径扩展出其他状态修改。
     */
    @DeleteMapping
    public AuthSessionResponse logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication != null) {
            securityAuditRecorder.loggedOut(request, authentication.getName());
        }
        logoutHandler.logout(request, response, authentication);
        return AuthSessionResponse.anonymous();
    }
}
