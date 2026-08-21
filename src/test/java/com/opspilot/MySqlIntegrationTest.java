package com.opspilot;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 所有 MySQL 集成测试的公共基类。
 *
 * <p>测试不使用 H2 模拟数据库，而是由 Testcontainers 启动真实 MySQL 8.4，
 * 因而能验证悲观锁、唯一索引、decimal 精度和 Flyway SQL 等数据库相关行为。
 * 代价是运行测试前必须启动 Docker Desktop。</p>
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} 很重要：Testcontainers 会在每个具体测试类结束后
 * 停止其容器，如果 Spring 继续复用仍指向旧动态端口的连接池，下一测试类就会连接已停止的
 * MySQL。每类结束后关闭上下文，可保证下一个类基于重新启动的容器建立新 DataSource。</p>
 */
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class MySqlIntegrationTest {

    /**
     * static 容器在继承本基类的一组测试间复用，减少重复启动 MySQL 的耗时。
     * {@code @ServiceConnection} 会把容器动态端口和凭据自动注入 Spring DataSource，
     * 避免手工拼接 JDBC URL。
     */
    @Container
    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("opspilot_test")
            .withUsername("opspilot")
            .withPassword("opspilot_test_password");
}
