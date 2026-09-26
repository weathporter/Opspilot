package com.opspilot.ledger;

import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountRepository;
import com.opspilot.account.CreateAccountRequest;
import com.opspilot.transfer.CreateTransferRequest;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferApplicationService;
import com.opspilot.transfer.TransferOrderRepository;
import com.opspilot.transfer.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 真实 MySQL 8.4 验证 Flyway、JdbcTemplate 与 JPA 是否共享事务、账户悲观锁和同键并发重放。
 * 本机 Docker 不可用时不能假称此测试通过；GitHub Actions Ubuntu runner 会执行。
 */
@SpringBootTest
@Testcontainers
class LedgerMysqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("northledger_ledger_test")
            .withUsername("ledger_test")
            .withPassword("ledger_test_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_PASSWORD", MYSQL::getPassword);
        registry.add("INTERNAL_ACCESS_TOKEN", () -> "access-test-secret");
        registry.add("INTERNAL_OPERATIONS_READ_TOKEN", () -> "operations-test-secret");
    }

    @Autowired AccountApplicationService accountService;
    @Autowired TransferApplicationService transferService;
    @Autowired AccountRepository accounts;
    @Autowired TransferOrderRepository orders;
    @Autowired LedgerEntryRepository entries;

    @BeforeEach
    void clean() {
        entries.deleteAll();
        orders.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void eightConcurrentReplaysCommitOneMoneyMovement() throws Exception {
        accountService.create(new CreateAccountRequest("12345678", "付款", new BigDecimal("100.00")));
        accountService.create(new CreateAccountRequest("87654321", "收款", BigDecimal.ZERO));
        CreateTransferRequest request = new CreateTransferRequest("12345678", "87654321",
                new BigDecimal("10.00"));
        int callers = 8;
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<TransferResponse>> futures = new ArrayList<>();
            for (int index = 0; index < callers; index++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("start timeout"); }
                    return transferService.transfer("parallel-key", request);
                }));
            }
            if (!ready.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("workers not ready"); }
            start.countDown();
            TransferResponse expected = futures.get(0).get(30, TimeUnit.SECONDS);
            for (Future<TransferResponse> future : futures) {
                assertEquals(expected, future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(0, accountService.get("12345678").balance().compareTo(new BigDecimal("90.00")));
        assertEquals(0, accountService.get("87654321").balance().compareTo(new BigDecimal("10.00")));
        assertEquals(1, orders.count());
        assertEquals(2, entries.count());
    }
}
