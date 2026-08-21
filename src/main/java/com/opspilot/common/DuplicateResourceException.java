package com.opspilot.common;

/**
 * 表示创建的资源或请求唯一标识已存在且产生冲突。
 * 典型场景包括重复账号，以及同一个幂等键被用于不同转账参数；API 层映射为 HTTP 409。
 */
public class DuplicateResourceException extends RuntimeException {

    /** 供客户端判断冲突类型、供监控按错误原因聚合的稳定错误码。 */
    private final String code;

    /** 构造不需要保留底层原因的冲突，例如主动识别出的幂等键冲突。 */
    public DuplicateResourceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造由基础设施异常转换而来的冲突。
     * 保留 cause 能让服务端日志看到 MySQL 唯一键异常，同时不把数据库细节暴露给 API 调用方。
     */
    public DuplicateResourceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** @return 稳定冲突错误码 */
    public String getCode() {
        return code;
    }
}
