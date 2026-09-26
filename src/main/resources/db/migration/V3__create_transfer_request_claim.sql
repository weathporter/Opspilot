-- 原单体环境已执行 V1/V2，因此通过 V3 向前添加同一并发幂等锁表。
CREATE TABLE transfer_request_claim (
    request_id VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
