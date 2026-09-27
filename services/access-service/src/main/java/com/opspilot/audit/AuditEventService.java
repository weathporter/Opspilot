package com.opspilot.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计写入与查询用例。
 *
 * <p>所有字符串在唯一入口限长，既保护数据库约束，也避免攻击者通过超长路径或用户名让
 * 原本用于记录攻击的审计功能反过来导致请求失败。</p>
 */
@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    public AuditEventService(AuditEventRepository auditEventRepository, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    /** 记录与当前 HTTP 请求关联的事件；只选取远端地址，不复制 Cookie、密码或请求体。 */
    @Transactional
    public void record(
            String eventType,
            AuditOutcome outcome,
            String actor,
            String targetType,
            String targetId,
            String description,
            HttpServletRequest request
    ) {
        AuditEvent event = AuditEvent.record(
                required(eventType, 64),
                outcome,
                optional(actor, 64),
                optional(targetType, 64),
                optional(targetId, 128),
                optional(MDC.get("traceId"), 64),
                optional(request == null ? null : request.getRemoteAddr(), 45),
                required(description, 255),
                LocalDateTime.now(clock)
        );
        auditEventRepository.save(event);
    }

    /** 限制单次最多 200 条，防止审计页误操作造成无界数据库读取。 */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 200));
        return auditEventRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit)).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    /** 必填值空白属于编程错误；尽早失败比写入无法解释的审计记录更安全。 */
    private static String required(String value, int maxLength) {
        String normalized = optional(value, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException("审计必填字段不能为空");
        }
        return normalized;
    }

    /** 可选值去除首尾空白并按数据库长度截断。 */
    private static String optional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
