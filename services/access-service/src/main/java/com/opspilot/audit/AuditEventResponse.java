package com.opspilot.audit;

import java.time.LocalDateTime;

/** 审计查询的外部 DTO；字段经过白名单选择，不会序列化 JPA 内部状态。 */
public record AuditEventResponse(
        Long id,
        String eventType,
        AuditOutcome outcome,
        String actor,
        String targetType,
        String targetId,
        String traceId,
        String sourceIp,
        String description,
        LocalDateTime occurredAt
) {
    /** 从持久化实体构造只读响应。 */
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getOutcome(),
                event.getActor(),
                event.getTargetType(),
                event.getTargetId(),
                event.getTraceId(),
                event.getSourceIp(),
                event.getDescription(),
                event.getOccurredAt()
        );
    }
}
