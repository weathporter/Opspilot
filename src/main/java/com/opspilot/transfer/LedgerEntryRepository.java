package com.opspilot.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 资金流水仓储：除批量写入外，还提供按订单读取双边审计证据的能力。 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * 沿实体关联使用 transferOrder.requestId 查询，并按主键顺序稳定返回借记、贷记记录。
     * 方法名由 Spring Data 解析为 SQL，无需在控制层拼接查询条件。
     */
    List<LedgerEntry> findByTransferOrderRequestIdOrderByIdAsc(String requestId);
}
