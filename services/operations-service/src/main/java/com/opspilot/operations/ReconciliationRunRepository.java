package com.opspilot.operations;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 最近批次有界读取；不提供普通用户可调用的修改/删除接口。 */
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
    List<ReconciliationRun> findAllByOrderByCompletedAtDesc(Pageable pageable);
}
