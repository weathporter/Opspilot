package com.opspilot.transfer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 旧 Compose/Linux 路径也按幂等键在 MySQL 中串行化，避免并发重放变成 500。 */
@Repository
public class TransferRequestClaimRepository {
    private final JdbcTemplate jdbc;

    public TransferRequestClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void acquire(String requestId) {
        jdbc.update("INSERT INTO transfer_request_claim (request_id) VALUES (?) "
                + "ON DUPLICATE KEY UPDATE request_id = request_id", requestId);
    }
}
