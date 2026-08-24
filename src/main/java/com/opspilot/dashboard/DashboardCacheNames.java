package com.opspilot.dashboard;

/**
 * Dashboard 模块声明自己拥有的缓存名称。
 *
 * <p>名称集中定义可以避免查询服务、失效监听器和监控代码各自拼写字符串；缓存仍属于
 * dashboard 读模型，而不是 Redis 配置模块，技术配置只能消费该稳定边界。</p>
 */
public final class DashboardCacheNames {

    /** 账户与交易聚合形成的当前运维总览快照。 */
    public static final String OPERATIONS_SUMMARY = "operations-summary";

    private DashboardCacheNames() {
        // 常量容器不允许实例化，避免把它误当作有状态服务注入。
    }
}
