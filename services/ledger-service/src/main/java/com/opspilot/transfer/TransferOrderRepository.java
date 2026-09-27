package com.opspilot.transfer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 转账订单仓储：幂等查询、控制台列表和运行汇总共用同一真实数据源。 */
public interface TransferOrderRepository extends JpaRepository<TransferOrder, Long> {

    /** request_id 唯一索引使幂等重放和结果查询都保持稳定效率。 */
    Optional<TransferOrder> findByRequestId(String requestId);

    /** 返回最近订单；Pageable 把数量限制下推到数据库。 */
    List<TransferOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 按状态返回最近订单，供前端“全部/已完成/处理中”切换。 */
    List<TransferOrder> findAllByStatusOrderByCreatedAtDesc(TransferStatus status, Pageable pageable);

    /** 查询某个时刻后的订单，用于按小时聚合近 24 小时交易趋势。 */
    List<TransferOrder> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    /** 统计指定状态订单数，避免运行总览依赖前端已加载的有限列表。 */
    long countByStatus(TransferStatus status);

    /** 汇总指定状态的交易金额，空集合时返回 0 而不是 null。 */
    @Query("select coalesce(sum(t.amount), 0) from TransferOrder t where t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") TransferStatus status);
}
