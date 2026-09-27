package com.opspilot.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 内部读模型不接受浏览器会话，也不能因不带服务令牌而匿名开放。 */
@WebMvcTest(OperationsSummaryController.class)
@Import(OperationsSecurityConfiguration.class)
@TestPropertySource(properties = "northledger.internal.access-token=test-access-service-secret")
class OperationsWebSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean OperationsSummaryService service;

    @Test
    void requiresAccessServiceToken() throws Exception {
        when(service.summary()).thenReturn(new ObjectMapper().createObjectNode().put("accountCount", 1));
        mvc.perform(get("/internal/v1/operations/summary"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/v1/operations/summary")
                        .header("Authorization", "Bearer test-access-service-secret"))
                .andExpect(status().isOk());
    }
}
