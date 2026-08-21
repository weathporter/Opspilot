package com.opspilot.common;

/**
 * 表示“请求能够解析、资源也存在，但操作违反领域规则”的异常。
 * 例如余额不足、冻结账户交易或付款/收款账号相同；API 层会统一映射为 HTTP 422。
 */
public class BusinessRuleException extends RuntimeException {

    /** 机器可读且稳定的错误码，避免调用方依赖可能调整的中文提示。 */
    private final String code;

    /**
     * @param code 稳定业务错误码，例如 INSUFFICIENT_BALANCE
     * @param message 面向用户或排障人员的可读描述
     */
    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 供 API 错误响应使用的稳定业务错误码 */
    public String getCode() {
        return code;
    }
}
