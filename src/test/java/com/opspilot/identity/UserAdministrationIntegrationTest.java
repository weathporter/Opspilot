package com.opspilot.identity;

import com.opspilot.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

/** 管理员创建与查看平台用户的 HTTP 契约。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdministrationIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 管理员可以创建操作员，但响应和列表都不能包含密码或密码哈希。 */
    @Test
    void administratorCanCreateAndListAUserWithoutCredentialLeakage() throws Exception {
        String requestBody = """
                {
                  "username": "operator01",
                  "password": "NorthLedger-Operator-123!",
                  "displayName": "资金操作员",
                  "roles": ["OPERATOR"]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("operator01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("NorthLedger-Operator-123!"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'operator01')]").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("password"))));
    }

    /** 操作员即使知道管理员接口地址也不能创建其他用户。 */
    @Test
    void operatorCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "illegal-admin",
                                  "password": "NorthLedger-Illegal-123!",
                                  "displayName": "越权用户",
                                  "roles": ["ADMIN"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
