package com.opspilot.security;

import com.opspilot.MySqlIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 身份认证与访问控制的端到端 HTTP 契约测试。
 *
 * <p>这里刻意使用完整 Spring 上下文、真实 MySQL 和 MockMvc：MockMvc 负责模拟浏览器的
 * HTTP 请求，Testcontainers 负责验证用户、会话和审计数据确实能落到 MySQL，而不是只在
 * Java 内存中“看起来能用”。每个用例都从攻击者或错误操作者可能采取的动作出发。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractIntegrationTest extends MySqlIntegrationTest {

    /** 进程内 HTTP 客户端；不会占用 18080 端口。 */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 登录页首次加载需要知道当前是否已认证，并取得 CSRF Cookie。
     * 因而会话查询是少数允许匿名访问的业务端点，但匿名响应绝不能伪造用户信息。
     */
    @Test
    void anonymousCallerCanReadOnlyAnonymousSessionState() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles").isEmpty());
    }

    /**
     * 未登录调用方不能读取账户清单。401 表示“尚未认证”，与已经登录但无权访问的
     * 403 严格区分，前端据此决定跳转登录页还是展示权限不足。
     */
    @Test
    void anonymousCallerCannotReadProtectedBusinessData() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    /**
     * 测试环境通过配置引导创建管理员。登录成功后服务端返回 SESSION Cookie，随后浏览器
     * 只需携带该 Cookie 就能恢复身份；密码本身不会进入 Cookie、localStorage 或响应体。
     */
    @Test
    void validCredentialsCreateAReusableServerSideSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/session")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "NorthLedger-Test-Only-123!"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn();

        Cookie sessionCookie = login.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).as("登录成功后必须返回不透明的服务端会话标识").isNotNull();

        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.displayName").value("Test Administrator"));
    }

    /**
     * 审计员可以查看账户，但不能创建账户。该测试证明权限控制发生在服务端，而不是
     * 仅依赖前端“隐藏按钮”；攻击者即使手工构造 POST 请求也只能得到 403。
     */
    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "accountNo": "6222000000008801",
                                  "holderName": "越权请求",
                                  "openingBalance": 100.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    /**
     * 即使操作者已经登录，缺少 CSRF Token 的跨站写请求仍必须被拒绝。
     * 这覆盖“诱导已登录用户在恶意页面发起转账/开户请求”的威胁场景。
     */
    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "accountNo": "6222000000008802",
                                  "holderName": "CSRF 请求",
                                  "openingBalance": 100.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
