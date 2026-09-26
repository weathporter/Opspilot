-- Operations 独占对账历史库。这里不保存账户余额、订单实体或可写资金事实。
CREATE TABLE reconciliation_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    completed_at TIMESTAMP(6) NOT NULL,
    checked_count INT NOT NULL,
    discrepancy_count INT NOT NULL,
    status VARCHAR(16) NOT NULL
);

CREATE INDEX idx_reconciliation_run_completed_at ON reconciliation_run (completed_at);

CREATE TABLE reconciliation_discrepancy (
    run_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_reconciliation_discrepancy PRIMARY KEY (run_id, request_id),
    CONSTRAINT fk_reconciliation_discrepancy_run FOREIGN KEY (run_id)
        REFERENCES reconciliation_run (id) ON DELETE CASCADE
);
