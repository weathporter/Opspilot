package com.opspilot.access;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 对账执行从统一登录入口发起，但真正历史数据只能由 Operations 保存。 */
class ReconciliationProxyControllerTest {
    @Test
    void runReturnsCreatedStatusFromOperations() throws Exception {
        OperationsHttpClient client = mock(OperationsHttpClient.class);
        when(client.runReconciliation(20, null)).thenReturn(ResponseEntity.status(201).body("{}".getBytes()));

        MockMvcBuilders.standaloneSetup(new ReconciliationProxyController(client)).build()
                .perform(post("/api/v1/operations/reconciliations").param("limit", "20"))
                .andExpect(status().isCreated());

        verify(client).runReconciliation(20, null);
    }
}
