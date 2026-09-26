package com.opspilot.common;

/**
 * 表示按业务标识没有找到所需资源。
 * 使用专用异常可让应用服务保持线性流程，并由统一异常处理器转换为 HTTP 404。
 */
public class ResourceNotFoundException extends RuntimeException {

    /** 区分 ACCOUNT_NOT_FOUND、TRANSFER_NOT_FOUND 等缺失类型。 */
    private final String code;

    /**
     * @param code 稳定资源错误码
     * @param message 可读的资源缺失描述
     */
    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 稳定资源错误码 */
    public String getCode() {
        return code;
    }
}
