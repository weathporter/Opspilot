package com.opspilot.audit;

/** 审计事件结果采用小而固定的枚举，便于筛选并避免描述文本承担机器语义。 */
public enum AuditOutcome {
    /** 请求完成并产生预期结果。 */
    SUCCESS,
    /** 请求执行失败，例如凭据错误或业务异常。 */
    FAILURE,
    /** 请求因权限策略被明确拒绝。 */
    DENIED
}
