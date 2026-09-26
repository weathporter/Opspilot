package com.opspilot.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 首次初始化的数据库级互斥点。两个 Access 副本可以同时启动，但相同主键的写入会串行执行。
 * 这条语句和创建管理员共用外层事务，后一个副本只有在前一个提交或回滚后才能检查用户数。
 */
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
