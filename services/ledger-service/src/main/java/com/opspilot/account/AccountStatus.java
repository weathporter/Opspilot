package com.opspilot.account;

/**
 * 账户生命周期中的可用状态。
 * 使用有限枚举而不是任意字符串，可以在编译期阻止拼写错误和未知状态进入业务逻辑。
 */
public enum AccountStatus {
    /** 正常账户，可以参与借记和贷记。 */
    ACTIVE,

    /** 冻结账户，任何资金变动都会由 Account 实体拒绝。 */
    FROZEN
}
