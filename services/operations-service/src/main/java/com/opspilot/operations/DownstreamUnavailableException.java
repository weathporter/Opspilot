package com.opspilot.operations;

/** 事实服务不可用或返回损坏数据时明确失败，不能用空统计伪装成功。 */
public class DownstreamUnavailableException extends RuntimeException {
    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
