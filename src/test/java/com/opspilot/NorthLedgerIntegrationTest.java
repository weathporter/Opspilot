package com.opspilot;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * NorthLedger 全栈集成测试的公共基础设施。
 *
 * <p>测试不使用 H2 或内存 Map 冒充生产依赖，而是启动真实 MySQL 8.4 与 Redis 7.4。
 * 因而数据库迁移、悲观锁、Decimal 精度、Redis TTL 和跨请求会话都能在接近真实的协议边界验证。
 * 代价是执行测试前必须启动 Docker Desktop。</p>
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} 防止某个具体测试类结束、容器停止以后，后续测试类
 * 错误复用仍指向旧动态端口的 Spring 连接池。每个测试类结束后重建上下文，换取更确定的隔离。</p>
 */
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class NorthLedgerIntegrationTest {

    /** 测试专用密码只保护临时容器，不进入运行镜像或生产配置。 */
    private static final String REDIS_TEST_PASSWORD = "northledger_test_redis_password";

    /**
     * {@code @ServiceConnection} 让 Spring Boot 从容器读取 JDBC URL、用户名和密码，
     * 避免测试代码手工拼接随机映射端口。
     */
    @Container
    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("opspilot_test")
            .withUsername("opspilot")
            .withPassword("opspilot_test_password");

    /**
     * Testcontainers 暂无本项目需要额外引入的 Redis 专用模块，通用容器已经足够表达协议边界。
     * 关闭 RDB/AOF 只针对一次性测试数据；Compose 和 Kubernetes 学习环境会启用 AOF。
     */
    @Container
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4.10-alpine")
    )
            .withExposedPorts(6379)
            .withCommand(
                    "redis-server",
                    "--requirepass", REDIS_TEST_PASSWORD,
                    "--save", "",
                    "--appendonly", "no"
            );

    /**
     * Redis 没有像 JDBC 那样的专用 {@code @ServiceConnection} 容器类型，因而把 Testcontainers
     * 的动态地址显式注入 Spring。属性值通过 Supplier 延迟读取，容器启动后才解析映射端口。
     */
    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.username", () -> "default");
        registry.add("spring.data.redis.password", () -> REDIS_TEST_PASSWORD);
        registry.add("spring.data.redis.connect-timeout", () -> "1s");
        registry.add("spring.data.redis.timeout", () -> "1s");
    }
}
