package com.opspilot.access;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 保持已有看板 URL，Access 自己不跨库统计。 */
class OperationsProxyControllerTest {
    @Test
    void routesSummaryToOperationsOnly() throws Exception {
        OperationsHttpClient client = mock(OperationsHttpClient.class);
        when(client.summary(any())).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body("{\"accountCount\":1}".getBytes()));

        MockMvcBuilders.standaloneSetup(new OperationsProxyController(client)).build()
                .perform(get("/api/v1/operations/summary"))
                .andExpect(status().isOk());

        verify(client).summary(any());
    }
}
