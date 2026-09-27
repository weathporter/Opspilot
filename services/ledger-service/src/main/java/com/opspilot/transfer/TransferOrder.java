package com.opspilot.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账订单实体及幂等记录，映射 {@code transfer_order} 表。
 * requestId 唯一约束把“某个请求是否已经执行”持久化到数据库，进程重启后仍然有效。
 */
@Entity
@Table(name = "transfer_order")
public class TransferOrder {

    /** 数据库内部自增主键，也是流水表引用的外键目标。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 客户端幂等键；唯一约束是应用层检查之外的并发最终防线。 */
    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    /** 付款账号快照。 */
    @Column(name = "source_account_no", nullable = false, length = 32)
    private String sourceAccountNo;

    /** 收款账号快照。 */
    @Column(name = "target_account_no", nullable = false, length = 32)
    private String targetAccountNo;

    /** 转账金额，精度与账户余额字段保持一致。 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** 订单状态使用字符串落库，方便 SQL 排障并避免枚举顺序变化。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    /** 订单开始处理时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 完成时间；处理中允许为空。 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** JPA 专用无参构造器，业务代码不能用它创建不完整订单。 */
    protected TransferOrder() {
    }

    /** 私有构造器强制新订单从 PROCESSING 状态开始。 */
    private TransferOrder(
            String requestId,
            String sourceAccountNo,
            String targetAccountNo,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
        this.requestId = requestId;
        this.sourceAccountNo = sourceAccountNo;
        this.targetAccountNo = targetAccountNo;
        this.amount = amount;
        this.status = TransferStatus.PROCESSING;
        this.createdAt = createdAt;
    }

    /** 创建尚未完成的转账订单，避免调用方自行组合初始状态。 */
    public static TransferOrder start(
            String requestId,
            String sourceAccountNo,
            String targetAccountNo,
            BigDecimal amount,
            LocalDateTime now
    ) {
        return new TransferOrder(requestId, sourceAccountNo, targetAccountNo, amount, now);
    }

    /** 在余额和流水处理完毕后把订单推进到 COMPLETED 并记录完成时间。 */
    public void complete(LocalDateTime now) {
        status = TransferStatus.COMPLETED;
        completedAt = now;
    }

    /**
     * 判断重试请求的业务载荷是否与原订单一致。
     * BigDecimal 使用 compareTo 而非 equals，使 10.0 与 10.00 按数值视为相同金额。
     */
    public boolean hasSamePayload(String sourceAccountNo, String targetAccountNo, BigDecimal amount) {
        return this.sourceAccountNo.equals(sourceAccountNo)
                && this.targetAccountNo.equals(targetAccountNo)
                && this.amount.compareTo(amount) == 0;
    }

    /** @return 订单数据库主键 */
    public Long getId() {
        return id;
    }

    /** @return 客户端幂等键 */
    public String getRequestId() {
        return requestId;
    }

    /** @return 付款账号 */
    public String getSourceAccountNo() {
        return sourceAccountNo;
    }

    /** @return 收款账号 */
    public String getTargetAccountNo() {
        return targetAccountNo;
    }

    /** @return 转账金额 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** @return 当前订单状态 */
    public TransferStatus getStatus() {
        return status;
    }

    /** @return 订单创建时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return 完成时间；PROCESSING 状态下可能为 null */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
