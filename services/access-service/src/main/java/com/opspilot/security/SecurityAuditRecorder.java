package com.opspilot.security;

import com.opspilot.audit.AuditEventService;
import com.opspilot.audit.AuditOutcome;
import com.opspilot.observability.AuthenticationTelemetry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 连接安全过滤器、持久化审计、结构化日志和 Prometheus 指标。
 *
 * <p>审计写入异常不能把一次本应返回 401/403 的安全响应变成 500，因此各入口都采用
 * “记录失败时输出 error 日志、但不改变原安全决策”的策略。数据库审计与日志同时存在，
 * 也让数据库短暂故障时仍保留排障线索。</p>
 */
@Component
public class SecurityAuditRecorder {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditRecorder.class);

    private final AuditEventService auditEventService;
    private final AuthenticationTelemetry authenticationTelemetry;

    public SecurityAuditRecorder(
            AuditEventService auditEventService,
            AuthenticationTelemetry authenticationTelemetry
    ) {
        this.auditEventService = auditEventService;
        this.authenticationTelemetry = authenticationTelemetry;
    }

    /** 记录一次成功认证；指标标签只使用固定 success 值。 */
    public void loginSucceeded(HttpServletRequest request, String username) {
        authenticationTelemetry.recordSuccess();
        recordSafely("LOGIN", AuditOutcome.SUCCESS, username, "USER", username, "登录成功", request);
    }

    /** 记录失败认证；只保留被尝试的用户名，不记录失败密码或具体认证异常。 */
    public void loginFailed(HttpServletRequest request, String rawUsername) {
        authenticationTelemetry.recordFailure();
        String username = rawUsername == null ? null : rawUsername.trim().toLowerCase(Locale.ROOT);
        recordSafely("LOGIN", AuditOutcome.FAILURE, username, "USER", username, "登录失败", request);
    }

    /** 记录已认证用户的越权或 CSRF 拒绝。 */
    public void accessDenied(HttpServletRequest request, String username) {
        recordSafely(
                "ACCESS_DENIED",
                AuditOutcome.DENIED,
                username,
                "HTTP_PATH",
                request.getRequestURI(),
                "访问被安全策略拒绝",
                request
        );
    }

    /** 在会话销毁前记录注销主体。 */
    public void loggedOut(HttpServletRequest request, String username) {
        recordSafely("LOGOUT", AuditOutcome.SUCCESS, username, "USER", username, "退出登录", request);
    }

    /** 审计失败只影响取证完整性，不允许绕过或扭曲原本的认证/授权 HTTP 结果。 */
    private void recordSafely(
            String eventType,
            AuditOutcome outcome,
            String actor,
            String targetType,
            String targetId,
            String description,
            HttpServletRequest request
    ) {
        try {
            auditEventService.record(eventType, outcome, actor, targetType, targetId, description, request);
            logger.atInfo()
                    .addKeyValue("event", eventType.toLowerCase(Locale.ROOT))
                    .addKeyValue("outcome", outcome.name().toLowerCase(Locale.ROOT))
                    .addKeyValue("actor", actor)
                    .log("security audit event recorded");
        } catch (RuntimeException exception) {
            // 不输出用户名以外的请求内容，也不输出任何凭据；堆栈留给内部日志排障。
            logger.atError()
                    .addKeyValue("event", "security_audit_write_failed")
                    .addKeyValue("auditEventType", eventType)
                    .setCause(exception)
                    .log("failed to persist security audit event");
        }
    }
}
