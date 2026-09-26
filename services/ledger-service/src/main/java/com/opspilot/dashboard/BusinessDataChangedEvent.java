package com.opspilot.dashboard;

/**
 * 通知只读总览“其依赖的 MySQL 业务事实已经发生变化”。
 *
 * <p>事件刻意只携带固定枚举，不携带账号、姓名、金额或请求体。缓存失效只需要知道变化类别，
 * 额外业务数据既没有用途，也会扩大日志、监听器和内存中的敏感数据暴露面。</p>
 *
 * @param changeType 触发总览重建的有界业务变化类型
 */
public record BusinessDataChangedEvent(ChangeType changeType) {

    /** 当前总览统计真正依赖的两类写事件。 */
    public enum ChangeType {
        ACCOUNT_CREATED,
        TRANSFER_COMPLETED
    }
}
