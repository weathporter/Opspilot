package com.opspilot.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 只追加安全/业务审计实体。
 *
 * <p>系统只提供构造与查询，不提供更新、删除领域方法或 HTTP 接口；这能防止普通管理操作
 * 抹掉登录失败和越权痕迹。更高等级生产环境还可把事件异步复制到外部不可变审计存储。</p>
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 稳定事件名，例如 LOGIN、LOGOUT、ACCESS_DENIED、ACCOUNT_CREATED。 */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditOutcome outcome;

    /** 行为主体；匿名事件可为空，失败登录保留被尝试的标准化用户名。 */
    @Column(length = 64)
    private String actor;

    /** 被操作对象的类型与业务标识，不记录整个请求体。 */
    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_id", length = 128)
    private String targetId;

    /** 与结构化日志中的请求 ID 一致，用于从审计记录跳转到本次请求日志。 */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /** 来源 IP 用于安全调查；不作为 Prometheus 标签，避免高基数。 */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /** 面向审计员的简短结论；绝不包含密码、Cookie、Token 或完整请求体。 */
    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** 仅供 JPA 使用。 */
    protected AuditEvent() {
    }

    private AuditEvent(
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
        this.eventType = eventType;
        this.outcome = outcome;
        this.actor = actor;
        this.targetType = targetType;
        this.targetId = targetId;
        this.traceId = traceId;
        this.sourceIp = sourceIp;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    /** 创建已经清洗和限长的审计事件。 */
    public static AuditEvent record(
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
        return new AuditEvent(eventType, outcome, actor, targetType, targetId,
                traceId, sourceIp, description, occurredAt);
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getActor() {
        return actor;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
