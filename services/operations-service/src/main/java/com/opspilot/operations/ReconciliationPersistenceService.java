package com.opspilot.operations;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 远端 Ledger 查询完成后才开启本地事务，避免等待网络时长期占用 MySQL 连接与锁。 */
@Service
public class ReconciliationPersistenceService {
    private final ReconciliationRunRepository repository;
    private final Clock clock;

    public ReconciliationPersistenceService(ReconciliationRunRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ReconciliationRunResponse persist(int checkedCount, List<String> discrepancies) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        return repository.save(ReconciliationRun.completed(checkedCount, discrepancies, now)).summary();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRunResponse> recent(int limit) {
        return repository.findAllByOrderByCompletedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(ReconciliationRun::summary).toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationDetailResponse detail(long id) {
        return repository.findById(id).map(ReconciliationRun::detail)
                .orElseThrow(() -> new ReconciliationNotFoundException(id));
    }
}
