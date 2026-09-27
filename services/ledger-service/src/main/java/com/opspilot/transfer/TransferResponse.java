package com.opspilot.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账 API 的只读响应快照，不向外暴露 JPA 主键和实体关联。
 *
 * @param requestId 幂等键，也是调用方查询交易状态的业务标识
 * @param sourceAccountNo 付款账号
 * @param targetAccountNo 收款账号
 * @param amount 转账金额
 * @param status 订单处理状态
 * @param createdAt 订单创建时间
 * @param completedAt 完成时间，处理中可能为空
 */
public record TransferResponse(
        String requestId,
        String sourceAccountNo,
        String targetAccountNo,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    /** 把事务中的订单实体转换为脱离持久化上下文也可安全使用的 API 数据。 */
    public static TransferResponse from(TransferOrder transferOrder) {
        return new TransferResponse(
                transferOrder.getRequestId(),
                transferOrder.getSourceAccountNo(),
                transferOrder.getTargetAccountNo(),
                transferOrder.getAmount(),
                transferOrder.getStatus(),
                transferOrder.getCreatedAt(),
                transferOrder.getCompletedAt()
        );
    }
}
