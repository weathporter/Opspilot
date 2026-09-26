package com.opspilot.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 返回给 API 调用方的账户只读快照。
 *
 * <p>record 自动生成构造器、访问器、equals/hashCode 和 toString；单独定义响应模型
 * 可以隐藏数据库主键、乐观锁版本等内部字段，避免 JPA 实体直接成为外部契约。</p>
 *
 * @param accountNo 业务账号
 * @param holderName 开户人姓名
 * @param balance 当前余额
 * @param status 当前账户状态
 * @param createdAt 创建时间
 * @param updatedAt 最近更新时间
 */
public record AccountResponse(
        String accountNo,
        String holderName,
        BigDecimal balance,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 在事务内把可变的 JPA 实体转换为 API 快照。
     *
     * @param account 已加载的账户实体
     * @return 与实体当前状态一致的响应对象
     */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNo(),
                account.getHolderName(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
