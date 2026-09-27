package com.opspilot.operations;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Operations 独占的对账执行历史；只记录差异线索，不复制可写资金事实。 */
@Entity
@Table(name = "reconciliation_run")
public class ReconciliationRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
    @Column(name = "checked_count", nullable = false)
    private int checkedCount;
    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount;
    @Column(nullable = false, length = 16)
    private String status;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reconciliation_discrepancy", joinColumns = @JoinColumn(name = "run_id"))
    @Column(name = "request_id", nullable = false, length = 64)
    private Set<String> discrepancyRequestIds = new LinkedHashSet<>();

    protected ReconciliationRun() {
    }

    private ReconciliationRun(int checkedCount, List<String> discrepancies, LocalDateTime completedAt) {
        this.checkedCount = checkedCount;
        this.discrepancyRequestIds = new LinkedHashSet<>(discrepancies);
        this.discrepancyCount = this.discrepancyRequestIds.size();
        this.status = discrepancyCount == 0 ? "BALANCED" : "DISCREPANCY";
        this.completedAt = completedAt;
    }

    public static ReconciliationRun completed(int checkedCount, List<String> discrepancies,
                                              LocalDateTime completedAt) {
        return new ReconciliationRun(checkedCount, discrepancies, completedAt);
    }

    public ReconciliationRunResponse summary() {
        return new ReconciliationRunResponse(id, completedAt, checkedCount, discrepancyCount, status);
    }

    public ReconciliationDetailResponse detail() {
        return new ReconciliationDetailResponse(id, completedAt, checkedCount, discrepancyCount,
                status, List.copyOf(discrepancyRequestIds));
    }
}
