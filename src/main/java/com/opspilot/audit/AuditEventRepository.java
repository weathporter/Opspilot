package com.opspilot.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 审计事件只开放新增和倒序读取所需仓储能力。 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** @return 按发生时间从新到旧的受限事件页。 */
    List<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
