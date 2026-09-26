package com.opspilot.ledger;

import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用真正的 Spring Security 过滤链验证服务边界，而非只测试凭据过滤器本身。 */
@WebMvcTest(AccountController.class)
@Import(LedgerSecurityConfiguration.class)
@TestPropertySource(properties = {
        "northledger.internal.access-token=local-access-test-token",
        "northledger.internal.operations-read-token=local-operations-read-test-token"
})
class LedgerWebSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AccountApplicationService accounts;

    @Test
    void rejectsAnonymousInternalCall() throws Exception {
        mvc.perform(get("/internal/v1/ledger/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsServiceCredentialButNotBrowserRoute() throws Exception {
        when(accounts.list(anyString(), anyInt())).thenReturn(List.of());
        mvc.perform(get("/internal/v1/ledger/accounts")
                        .header("Authorization", "Bearer local-access-test-token"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer local-access-test-token"))
                // 凭据过滤器只认可内部路径；外部路径既无映射，也不会得到服务身份。
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operationsReadCredentialCannotCreateAccount() throws Exception {
        when(accounts.list(anyString(), anyInt())).thenReturn(List.of());
        mvc.perform(get("/internal/v1/ledger/accounts")
                        .header("Authorization", "Bearer local-operations-read-test-token"))
                // 运维服务只能访问将新增的专用统计端点，不应读取账户明细。
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/internal/v1/ledger/accounts")
                        .header("Authorization", "Bearer local-operations-read-test-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"accountNo\":\"A001\",\"holderName\":\"Test\",\"openingBalance\":100}"))
                .andExpect(status().isForbidden());
    }
}
