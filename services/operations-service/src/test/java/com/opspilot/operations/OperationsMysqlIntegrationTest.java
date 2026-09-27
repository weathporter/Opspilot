package com.opspilot.operations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 真实 MySQL 8.4 验证独立 V1 迁移及对账差异历史可读回，而非内存中假记录。 */
@SpringBootTest
@Testcontainers
class OperationsMysqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("northledger_operations_test")
            .withUsername("operations_test")
            .withPassword("operations_test_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_PASSWORD", MYSQL::getPassword);
        registry.add("REDIS_PASSWORD", () -> "operations_test_redis_password");
        registry.add("INTERNAL_ACCESS_TOKEN", () -> "access-to-operations-test");
        registry.add("INTERNAL_OPERATIONS_READ_TOKEN", () -> "operations-to-ledger-test");
    }

    @MockitoBean LedgerStatisticsGateway ledger;
    @Autowired ReconciliationPersistenceService persistence;

    @Test
    void savesAndReadsDiscrepancyFromIndependentDatabase() {
        ReconciliationRunResponse run = persistence.persist(2, List.of("mismatch-1"));
        ReconciliationDetailResponse detail = persistence.detail(run.id());

        assertEquals(2, detail.checkedCount());
        assertEquals(1, detail.discrepancyCount());
        assertEquals("DISCREPANCY", detail.status());
        assertEquals(List.of("mismatch-1"), detail.discrepancyRequestIds());
    }
}
