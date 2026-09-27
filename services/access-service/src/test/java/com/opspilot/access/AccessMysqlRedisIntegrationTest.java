package com.opspilot.access;

import com.opspilot.identity.AppUserRepository;
import com.opspilot.identity.BootstrapAdministrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用真实 MySQL/Redis 验证 Access 独立迁移、引导管理员、共享会话和匿名权限边界。 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccessMysqlRedisIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("northledger_access_test")
            .withUsername("access_test")
            .withPassword("access_test_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4.10-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "access_test_redis_password",
                    "--save", "", "--appendonly", "no");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_PASSWORD", MYSQL::getPassword);
        registry.add("REDIS_PASSWORD", () -> "access_test_redis_password");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("INTERNAL_ACCESS_TOKEN", () -> "test-access-token");
        registry.add("INTERNAL_OPERATIONS_TOKEN", () -> "test-operations-token");
        registry.add("BOOTSTRAP_ADMIN_USERNAME", () -> "admin");
        registry.add("BOOTSTRAP_ADMIN_PASSWORD", () -> "Access-Test-Only-123!");
    }

    @Autowired MockMvc mvc;
    @Autowired BootstrapAdministrator bootstrap;
    @Autowired AppUserRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void anonymousSessionWorksButMoneyRouteRequiresLogin() throws Exception {
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentAccessReplicasCreateOnlyOneInitialAdministrator() throws Exception {
        // 仅清空此 Testcontainers 临时库；两个线程模拟两个 Pod 同时进入首次启动引导。
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM bootstrap_claim");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = workers.submit(() -> runBootstrap(start));
            Future<?> second = workers.submit(() -> runBootstrap(start));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
        }
        assertEquals(1L, users.count());
    }

    private void runBootstrap(CountDownLatch start) {
        try {
            start.await(5, TimeUnit.SECONDS);
            bootstrap.run(new DefaultApplicationArguments());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
