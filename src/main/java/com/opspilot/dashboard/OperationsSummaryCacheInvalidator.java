package com.opspilot.dashboard;

import com.opspilot.observability.CacheFailureTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在业务写事务提交成功后清理总览快照。
 *
 * <p>{@link TransactionalEventListener} 默认绑定发布事件的数据库事务；这里显式选择
 * {@code AFTER_COMMIT}，保证回滚事务不会删除仍然正确的缓存。官方依据：
 * https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html</p>
 */
@Component
public class OperationsSummaryCacheInvalidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsSummaryCacheInvalidator.class);

    private final CacheManager cacheManager;
    private final CacheFailureTelemetry cacheFailureTelemetry;

    public OperationsSummaryCacheInvalidator(
            CacheManager cacheManager,
            CacheFailureTelemetry cacheFailureTelemetry
    ) {
        this.cacheManager = cacheManager;
        this.cacheFailureTelemetry = cacheFailureTelemetry;
    }

    /**
     * 事务已经提交后清空唯一总览键；如果未来增加其他 dashboard 缓存，也必须逐项评估依赖关系，
     * 不能为了方便执行 Redis 全库清空。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void invalidateAfterCommit(BusinessDataChangedEvent event) {
        Cache cache = cacheManager.getCache(DashboardCacheNames.OPERATIONS_SUMMARY);
        if (cache == null) {
            // 缓存名在启动时预创建；缺失意味着配置不一致，应显式失败并由测试/监控发现。
            throw new IllegalStateException("operations-summary cache is not configured");
        }
        try {
            cache.clear();
        } catch (RuntimeException exception) {
            // 数据库已经提交，缓存清理失败不能伪装成资金事务失败；由 TTL 与告警完成后续收敛。
            cacheFailureTelemetry.handleCacheClearError(exception, cache);
            return;
        }
        LOGGER.atInfo()
                .addKeyValue("event", "operations_summary_cache_evicted")
                .addKeyValue("change_type", event.changeType().name())
                .log("operations summary cache evicted after database commit");
    }
}
