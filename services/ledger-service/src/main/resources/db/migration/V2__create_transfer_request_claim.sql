-- 与 Ledger V1 同库；只新增表，不改写已执行的订单/流水迁移。
-- INSERT ... ON DUPLICATE KEY UPDATE 在事务内锁定同一幂等键，序列化并发重放。
CREATE TABLE transfer_request_claim (
    request_id VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
