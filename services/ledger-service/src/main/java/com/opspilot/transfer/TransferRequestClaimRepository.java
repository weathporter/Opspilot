package com.opspilot.transfer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * MySQL 键级串行化：同一 request_id 的第二个 INSERT/UPDATE 会等第一个事务提交或回滚。
 * 它与余额/订单/流水共用外层事务，失败时一起回滚；不同键互不阻塞。
 */
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
