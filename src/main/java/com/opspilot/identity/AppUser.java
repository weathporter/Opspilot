package com.opspilot.identity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 平台登录用户实体，对应 {@code app_user} 与 {@code app_user_role} 两张表。
 *
 * <p>密码字段只允许保存 BCrypt 密文，且本类故意不提供密码 getter，降低业务响应或日志
 * 意外暴露密文的概率。角色使用单独集合表，使一个用户可以同时承担管理与审计职责。</p>
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    /** 数据库内部标识；外部登录使用稳定但可审计的 username。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录名在写入前统一转为小写，数据库唯一约束负责并发场景最终防重。 */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** BCrypt 哈希；永不存储或返回明文密码。 */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 面向人的显示名称，可以与稳定登录名分开调整。 */
    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    /** ACTIVE/LOCKED/DISABLED 映射为可读字符串，避免枚举顺序变化破坏历史数据。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;

    /** 权限判断发生在每次请求，使用 EAGER 保证离开事务后仍有完整角色集合。 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);

    /** 创建与最近变更时间用于用户治理和审计追踪。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 仅供 JPA 反射还原实体。 */
    protected AppUser() {
    }

    /** 私有构造器保证新用户始终拥有状态、角色和完整时间字段。 */
    private AppUser(
            String username,
            String passwordHash,
            String displayName,
            Set<UserRole> roles,
            LocalDateTime now
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = UserStatus.ACTIVE;
        this.roles = EnumSet.copyOf(roles);
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 创建已完成边界校验且密码已经哈希的用户。
     *
     * @param username 已标准化的小写登录名
     * @param passwordHash BCrypt 密文
     * @param displayName 展示名
     * @param roles 至少一个角色
     * @param now 统一业务时间
     */
    public static AppUser create(
            String username,
            String passwordHash,
            String displayName,
            Set<UserRole> roles,
            LocalDateTime now
    ) {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("用户至少需要一个角色");
        }
        return new AppUser(username, passwordHash, displayName, roles, now);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    /** 仅认证适配器读取哈希；禁止把该返回值放入任何 DTO、日志或监控标签。 */
    String passwordHashForAuthentication() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    /** 返回不可修改视图，防止 Controller 绕过领域方法篡改权限。 */
    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
