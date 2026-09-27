package com.opspilot.ledger;

import com.opspilot.account.AccountApplicationService;
import com.opspilot.account.AccountController;
import com.opspilot.transfer.TransferApplicationService;
import com.opspilot.transfer.TransferController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 账务 Controller 只能挂在内部路由；不能因代码复用把原浏览器路由暴露到集群。 */
class LedgerHttpBoundaryTest {

    @Test
    void servesOnlyInternalAccountAndTransferRoutes() throws Exception {
        AccountApplicationService accounts = mock(AccountApplicationService.class);
        TransferApplicationService transfers = mock(TransferApplicationService.class);
        when(accounts.list(anyString(), anyInt())).thenReturn(List.of());
        when(transfers.list(org.mockito.ArgumentMatchers.<com.opspilot.transfer.TransferStatus>isNull(), anyInt()))
                .thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new AccountController(accounts), new TransferController(transfers))
                .addFilters(new InternalServiceCredentialFilter(
                        "test-access-secret", "test-operations-secret"))
                .build();

        mvc.perform(get("/internal/v1/ledger/accounts")
                        .header("Authorization", "Bearer test-access-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/internal/v1/ledger/transfers")
                        .header("Authorization", "Bearer test-access-secret"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer test-access-secret"))
                .andExpect(status().isNotFound());
    }
}
