package com.opspilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用启动与就绪探针的最小集成冒烟测试。
 *
 * <p>{@code @SpringBootTest} 启动完整 Spring 上下文，包含 Flyway、JPA、Actuator；
 * MockMvc 在进程内模拟 HTTP 请求，不需要真正占用本机端口。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeIntegrationTest extends MySqlIntegrationTest {

    /** 用于执行 MVC 请求并断言 HTTP 层响应。 */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证 readiness 端点可用且状态为 UP。
     * 如果此测试失败，应先检查 Spring 上下文、MySQL 容器、Flyway 迁移和 Actuator 配置，
     * 因为部署脚本、Docker healthcheck 和 Nginx 都依赖同一个就绪端点。
     */
    @Test
    void readinessEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
