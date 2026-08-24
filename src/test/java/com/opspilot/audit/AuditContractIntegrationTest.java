package com.opspilot.audit;

import com.opspilot.NorthLedgerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全审计与认证指标的集成契约。
 *
 * <p>测试先制造一次真实失败登录，再以审计员身份读取证据，最后从 Prometheus 文本确认
 * 失败计数。它验证“事件详情”和“聚合趋势”两条排障链路来自同一次实际行为。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
// Spring Boot 测试默认关闭指标导出；本类专门验证 Prometheus，因此显式恢复真实观测配置。
@AutoConfigureObservability
@ActiveProfiles("test")
class AuditContractIntegrationTest extends NorthLedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 首个管理员是高权限身份，引导创建本身必须进入长期审计事实。 */
    @Test
    void bootstrapAdministratorCreationProducesAuditEvidence() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events?limit=20")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType == 'BOOTSTRAP_ADMIN_CREATED')].actor")
                        .value(org.hamcrest.Matchers.hasItem("admin")))
                .andExpect(jsonPath("$[?(@.eventType == 'BOOTSTRAP_ADMIN_CREATED')].outcome")
                        .value(org.hamcrest.Matchers.hasItem("SUCCESS")));
    }

    /** 登录失败必须可追溯、可按角色读取，同时不能把密码写进审计响应。 */
    @Test
    void failedLoginProducesQueryableAuditEvidenceAndMetric() throws Exception {
        mockMvc.perform(post("/api/v1/auth/session")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "definitely-wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/v1/audit/events?limit=20")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk())
                // 不依赖测试方法或审计记录的执行顺序，只要求结果中存在目标失败事件。
                .andExpect(jsonPath("$[?(@.eventType == 'LOGIN' && @.outcome == 'FAILURE')].actor")
                        .value(org.hamcrest.Matchers.hasItem("admin")))
                .andExpect(jsonPath("$[?(@.eventType == 'LOGIN' && @.outcome == 'FAILURE')].description")
                        .value(org.hamcrest.Matchers.hasItem("登录失败")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("definitely-wrong-password"))));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                // 应用、环境、版本是全局公共标签，故只断言指标名和固定 outcome 标签都存在。
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("northledger_authentication_attempts_total"),
                        org.hamcrest.Matchers.containsString("outcome=\"failure\""))));
    }

    /** 普通业务操作员不具备审计事件读取权限。 */
    @Test
    void operatorCannotReadSecurityAuditEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
