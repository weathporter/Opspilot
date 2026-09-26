package com.opspilot.ledger;

import com.opspilot.account.AccountRepository;
import com.opspilot.transfer.TransferOrder;
import com.opspilot.transfer.TransferOrderRepository;
import com.opspilot.transfer.TransferResponse;
import com.opspilot.transfer.TransferStatus;
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

/** 只从本服务拥有的两类事实表构建统计快照，供 Operations 的只读投影使用。 */
@Service
public class LedgerStatisticsService {
    private final AccountRepository accounts;
    private final TransferOrderRepository transfers;
    private final Clock clock;

    public LedgerStatisticsService(AccountRepository accounts, TransferOrderRepository transfers, Clock clock) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LedgerStatisticsResponse snapshot() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        long count = transfers.count();
        long completed = transfers.countByStatus(TransferStatus.COMPLETED);
        double successRate = count == 0 ? 100.0 : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();
        List<TransferResponse> recent = transfers.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 6))
                .stream().map(TransferResponse::from).toList();
        return new LedgerStatisticsResponse(now, accounts.count(), accounts.sumBalance(), count, completed,
                transfers.sumAmountByStatus(TransferStatus.COMPLETED), successRate, trend(now), recent);
    }

    /** 预建 24 个零值桶，保持无交易时图表仍有连续的整点坐标。 */
    private List<HourlyTransferTrend> trend(LocalDateTime now) {
        LocalDateTime firstHour = now.truncatedTo(ChronoUnit.HOURS).minusHours(23);
        Map<LocalDateTime, MutableHour> buckets = new LinkedHashMap<>();
        for (int index = 0; index < 24; index++) {
            buckets.put(firstHour.plusHours(index), new MutableHour());
        }
        for (TransferOrder order : transfers.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(firstHour)) {
            MutableHour hour = buckets.get(order.getCreatedAt().truncatedTo(ChronoUnit.HOURS));
            if (hour == null) {
                continue;
            }
            if (order.getStatus() == TransferStatus.COMPLETED) {
                hour.completedCount++;
                hour.completedAmount = hour.completedAmount.add(order.getAmount());
            } else {
                hour.processingCount++;
            }
        }
        List<HourlyTransferTrend> result = new ArrayList<>(24);
        buckets.forEach((at, value) -> result.add(new HourlyTransferTrend(
                at, value.completedCount, value.processingCount, value.completedAmount)));
        return result;
    }

    /** 聚合过程中使用的私有状态，不会作为共享缓存对象暴露到服务外。 */
    private static final class MutableHour {
        private long completedCount;
        private long processingCount;
        private BigDecimal completedAmount = BigDecimal.ZERO;
    }
}
