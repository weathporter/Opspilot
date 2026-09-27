CREATE TABLE account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(32) NOT NULL,
    holder_name VARCHAR(64) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_account_no UNIQUE (account_no),
    CONSTRAINT ck_account_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE transfer_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    source_account_no VARCHAR(32) NOT NULL,
    target_account_no VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6),
    CONSTRAINT uk_transfer_request_id UNIQUE (request_id),
    CONSTRAINT ck_transfer_amount_positive CHECK (amount > 0)
);

CREATE TABLE ledger_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_order_id BIGINT NOT NULL,
    account_no VARCHAR(32) NOT NULL,
    entry_type VARCHAR(8) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ledger_transfer_order
        FOREIGN KEY (transfer_order_id) REFERENCES transfer_order (id),
    CONSTRAINT ck_ledger_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_ledger_account_created_at ON ledger_entry (account_no, created_at);
CREATE INDEX idx_transfer_source_created_at ON transfer_order (source_account_no, created_at);
