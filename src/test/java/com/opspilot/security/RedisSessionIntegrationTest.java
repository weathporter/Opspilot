package com.opspilot.security;

import com.opspilot.NorthLedgerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证登录态已经从 MySQL JDBC 会话表迁移到 Redis。
 *
 * <p>这个测试检查用户能观察到的真实边界，而不是只检查某一行 YAML：浏览器完成一次登录后，
 * Redis 必须出现带过期时间的会话键，同时历史 {@code SPRING_SESSION} 表不能继续增长。
 * 如果有人把依赖或配置误改回 JDBC，本测试会立即失败。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedisSessionIntegrationTest extends NorthLedgerIntegrationTest {

    /** MockMvc 走完整 Spring Security 过滤链，但不占用本机 HTTP 端口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 直接观察 Redis 持久化边界；只在测试中扫描固定命名空间，不把 KEYS 用到生产代码。 */
    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 用于证明旧 JDBC 会话表没有新增记录，业务测试仍连接真实 Testcontainers MySQL。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void successfulLoginStoresExpiringSessionInRedisInsteadOfJdbcTables() throws Exception {
        mockMvc.perform(post("/api/v1/auth/session")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", "admin")
                        .param("password", "NorthLedger-Test-Only-123!"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"));

        // Spring Session Redis 会创建主会话键、过期索引等内部键；固定前缀证明命名空间正确。
        List<String> sessionKeys = redisTemplate.keys("northledger:session:*")
                .stream()
                .sorted()
                .toList();
        assertThat(sessionKeys)
                .as("登录后 Redis 应出现 northledger:session 命名空间键")
                .isNotEmpty();

        // 至少一个会话对象必须拥有正 TTL；永久键会让退出后的身份数据无限期残留。
        boolean hasExpiringSession = sessionKeys.stream()
                .map(redisTemplate::getExpire)
                .anyMatch(ttlSeconds -> ttlSeconds != null && ttlSeconds > 0);
        assertThat(hasExpiringSession)
                .as("Redis 会话必须随 SESSION_TIMEOUT 自动过期")
                .isTrue();

        Integer jdbcSessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION",
                Integer.class
        );
        assertThat(jdbcSessionCount)
                .as("0.3.0 只保留历史 JDBC 表结构，不再向其中写会话")
                .isZero();
    }
}
