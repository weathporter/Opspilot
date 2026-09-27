package com.opspilot.access;

/** 内部服务网络故障；不得伪造成功或把低层异常暴露给浏览器。 */
public class DownstreamUnavailableException extends RuntimeException {
    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
