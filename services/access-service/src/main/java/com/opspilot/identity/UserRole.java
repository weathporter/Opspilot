package com.opspilot.identity;

/**
 * 平台采用的四类职责角色。
 *
 * <p>角色表达“能做什么”，不等同于岗位名称。Spring Security 按约定在角色前添加
 * {@code ROLE_}；把转换集中在 {@link #authority()} 可避免各处手工拼接造成权限漂移。</p>
 */
public enum UserRole {
    /** 系统管理员：维护用户、角色，并可查看所有审计证据。 */
    ADMIN,
    /** 业务操作员：开户、转账和日常业务查询。 */
    OPERATOR,
    /** 审计员：只读查询账户、交易、流水与安全审计事件。 */
    AUDITOR,
    /** 客户角色预留；在账户归属关系建立前不授予全量业务查询权。 */
    CUSTOMER;

    /** @return Spring Security 使用的完整权限字符串。 */
    public String authority() {
        return "ROLE_" + name();
    }
}
