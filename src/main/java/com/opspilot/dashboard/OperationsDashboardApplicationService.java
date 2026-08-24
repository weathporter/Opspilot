package com.opspilot.dashboard;

import com.opspilot.account.AccountRepository;
import com.opspilot.transfer.TransferOrder;
import com.opspilot.transfer.TransferOrderRepository;
import com.opspilot.transfer.TransferResponse;
import com.opspilot.transfer.TransferStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面向运维驾驶舱的查询服务。
 *
 * <p>它只读现有账户和订单，不修改资金数据；把跨模块汇总放在独立 dashboard 包中，
 * 避免账户模块和转账模块互相承担展示层职责。</p>
 */
@Service
public class OperationsDashboardApplicationService {

    private final AccountRepository accountRepository;
    private final TransferOrderRepository transferOrderRepository;
    private final Clock clock;
    private final String environment;
    private final String version;

    public OperationsDashboardApplicationService(
            AccountRepository accountRepository,
            TransferOrderRepository transferOrderRepository,
            Clock clock,
            @Value("${info.app.environment:local}") String environment,
            @Value("${info.app.version:dev}") String version
    ) {
        this.accountRepository = accountRepository;
        this.transferOrderRepository = transferOrderRepository;
        this.clock = clock;
        this.environment = environment;
        this.version = version;
    }

    /**
     * 以数据库统计和固定 24 小时窗口生成一次一致的运行快照。
     *
     * <p>总览是可重建的只读投影，允许 30～40 秒最终一致，因此可以使用 Redis 降低重复聚合查询。
     * 固定键 {@code current} 表示当前接口没有租户或筛选维度；账户/转账写入会在事务提交后主动失效，
     * TTL 则负责处理漏失效和 Redis 清理失败。资金事实本身始终只写 MySQL。</p>
     */
    @Cacheable(cacheNames = DashboardCacheNames.OPERATIONS_SUMMARY, key = "'current'")
    @Transactional(readOnly = true)
    public OperationsSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        long transferCount = transferOrderRepository.count();
        long completedCount = transferOrderRepository.countByStatus(TransferStatus.COMPLETED);
        double successRate = transferCount == 0
                ? 100.0
                : BigDecimal.valueOf(completedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(transferCount), 2, RoundingMode.HALF_UP)
                        .doubleValue();

        List<TransferResponse> recentTransfers = transferOrderRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 6))
                .stream()
                .map(TransferResponse::from)
                .toList();

        return new OperationsSummaryResponse(
                now,
                environment,
                version,
                accountRepository.count(),
                accountRepository.sumBalance(),
                transferCount,
                completedCount,
                transferOrderRepository.sumAmountByStatus(TransferStatus.COMPLETED),
                successRate,
                buildTrend(now),
                recentTransfers
        );
    }

    /**
     * 预先建立 24 个零值桶，再把订单归入对应整点。
     * 即使某个小时没有交易，前端折线也会连续而不会缺失时间轴。
     */
    private List<TransferTrendPoint> buildTrend(LocalDateTime now) {
        LocalDateTime currentHour = now.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime firstHour = currentHour.minusHours(23);
        Map<LocalDateTime, MutableTrend> buckets = new LinkedHashMap<>();
        for (int index = 0; index < 24; index++) {
            buckets.put(firstHour.plusHours(index), new MutableTrend());
        }

        List<TransferOrder> orders = transferOrderRepository
                .findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(firstHour);
        for (TransferOrder order : orders) {
            LocalDateTime hour = order.getCreatedAt().truncatedTo(ChronoUnit.HOURS);
            MutableTrend bucket = buckets.get(hour);
            if (bucket == null) {
                continue;
            }
            if (order.getStatus() == TransferStatus.COMPLETED) {
                bucket.completedCount++;
                bucket.completedAmount = bucket.completedAmount.add(order.getAmount());
            } else {
                bucket.processingCount++;
            }
        }

        List<TransferTrendPoint> result = new ArrayList<>(24);
        buckets.forEach((hour, bucket) -> result.add(new TransferTrendPoint(
                hour,
                bucket.completedCount,
                bucket.processingCount,
                bucket.completedAmount
        )));
        return result;
    }

    /** 仅在一次方法调用内使用的可变聚合容器，不会暴露到模块外部。 */
    private static final class MutableTrend {
        private long completedCount;
        private long processingCount;
        private BigDecimal completedAmount = BigDecimal.ZERO;
    }
}
