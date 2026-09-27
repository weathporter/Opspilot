package com.opspilot.account;

import com.opspilot.common.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户领域实体，同时映射 MySQL 的 {@code account} 表。
 *
 * <p>实体不只是数据库字段的容器：余额增减和账户状态校验都收口在这里，
 * 从而保证任何调用方都不能绕过“不允许冻结账户交易、不能透支”等核心规则。</p>
 *
 * <p>金额统一使用 {@link BigDecimal}，避免 {@code double} 的二进制浮点误差。
 * 在银行类项目中，这是“数据正确性优先于写法简短”的典型设计。</p>
 */
@Entity
@Table(name = "account")
public class Account {

    /** 数据库自增主键，只用于内部关联，不把可变的业务账号当作主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外使用的账号；数据库唯一约束是并发创建相同账号时的最终防线。 */
    @Column(name = "account_no", nullable = false, unique = true, length = 32)
    private String accountNo;

    /** 开户人姓名；长度限制与 SQL 表结构保持一致，避免 ORM 与数据库规则漂移。 */
    @Column(name = "holder_name", nullable = false, length = 64)
    private String holderName;

    /** 当前余额；precision=19、scale=2 表示最多 17 位整数和 2 位小数。 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * 账户状态以字符串保存。相比枚举序号，字符串更容易排障，也不会因枚举调整顺序而误读旧数据。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    /**
     * JPA 乐观锁版本号。每次更新会自动递增；若两个事务基于同一旧版本更新，后提交者会失败。
     * 转账主流程还使用悲观锁，本字段则作为其他更新入口的额外并发保护。
     */
    @Version
    @Column(nullable = false)
    private long version;

    /** 创建时间，由应用的 Clock 统一提供，便于测试时替换时间源。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 最近一次业务变更时间，借记和贷记成功后都会刷新。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 通过反射还原实体时需要无参构造器。
     * 使用 protected 可满足框架要求，同时阻止普通业务代码创建字段不完整的账户。
     */
    protected Account() {
    }

    /** 私有构造器只允许工厂方法调用，确保新账户始终处于 ACTIVE 且时间字段完整。 */
    private Account(String accountNo, String holderName, BigDecimal openingBalance, LocalDateTime now) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.balance = openingBalance;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 开立账户的领域工厂。
     *
     * @param accountNo 业务账号
     * @param holderName 开户人姓名
     * @param openingBalance 开户余额，非负校验在 API 请求对象完成
     * @param now 由应用服务传入的统一业务时间
     * @return 还未持久化的新账户
     */
    public static Account open(String accountNo, String holderName, BigDecimal openingBalance, LocalDateTime now) {
        return new Account(accountNo, holderName, openingBalance, now);
    }

    /**
     * 从账户扣款。先校验状态和余额，再同时变更余额与更新时间。
     * 抛出的运行时业务异常会使外层 {@code @Transactional} 自动回滚。
     *
     * @param amount 扣款金额，调用前已通过 Bean Validation 保证大于零
     * @param now 本次转账统一使用的时间点
     */
    public void debit(BigDecimal amount, LocalDateTime now) {
        ensureActive();
        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_BALANCE", "账户余额不足");
        }
        balance = balance.subtract(amount);
        updatedAt = now;
    }

    /**
     * 向账户入账。状态校验放在实体内部，避免冻结账户通过其他调用入口被误操作。
     *
     * @param amount 入账金额
     * @param now 本次转账统一使用的时间点
     */
    public void credit(BigDecimal amount, LocalDateTime now) {
        ensureActive();
        balance = balance.add(amount);
        updatedAt = now;
    }

    /** 校验账户是否可交易；失败时返回稳定错误码，便于前端判断和监控聚合。 */
    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("ACCOUNT_NOT_ACTIVE", "账户当前不可用");
        }
    }

    /** @return 数据库内部主键 */
    public Long getId() {
        return id;
    }

    /** @return 对外业务账号 */
    public String getAccountNo() {
        return accountNo;
    }

    /** @return 开户人姓名 */
    public String getHolderName() {
        return holderName;
    }

    /** @return 当前余额；BigDecimal 本身不可变，返回引用不会直接修改实体 */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @return 当前账户状态 */
    public AccountStatus getStatus() {
        return status;
    }

    /** @return 账户创建时间 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return 最近业务更新时间 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
