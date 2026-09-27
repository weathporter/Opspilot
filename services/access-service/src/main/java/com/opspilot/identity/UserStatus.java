package com.opspilot.identity;

/** 用户生命周期状态；认证组件据此决定账号是否允许登录。 */
public enum UserStatus {
    /** 正常使用。 */
    ACTIVE,
    /** 临时锁定，通常用于多次登录失败或人工处置。 */
    LOCKED,
    /** 已停用，保留审计关联但拒绝继续登录。 */
    DISABLED
}
