package com.opspilot.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 旧双副本路径与新 Access 路径保持同一防竞态语义：先锁固定行，再检查身份库。 */
@Repository
public class BootstrapClaimRepository {
    private final JdbcTemplate jdbc;

    public BootstrapClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void acquire() {
        jdbc.update("INSERT INTO bootstrap_claim (id) VALUES (1) "
                + "ON DUPLICATE KEY UPDATE id = id");
    }
}
