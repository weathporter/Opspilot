package com.opspilot.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 浏览器 URL 必须继续兼容旧前端，而目标始终是代码内的固定 Ledger 路径。 */
class BusinessProxyControllerTest {
    private LedgerHttpClient ledger;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ledger = mock(LedgerHttpClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new BusinessProxyController(ledger)).build();
    }

    @Test
    void accountListKeepsPublicUrlAndTypedQuery() throws Exception {
        when(ledger.exchange(eq(HttpMethod.GET), eq("/internal/v1/ledger/accounts"), any(),
                eq(null), eq(null), any())).thenReturn(ResponseEntity.ok("[]".getBytes()));

        mvc.perform(get("/api/v1/accounts").param("query", "123").param("limit", "20"))
                .andExpect(status().isOk());

        verify(ledger).exchange(eq(HttpMethod.GET), eq("/internal/v1/ledger/accounts"),
                eq(Map.of("query", "123", "limit", "20")), eq(null), eq(null), any());
    }

    @Test
    void transferWritePreservesIdempotencyKeyAndCreatedStatus() throws Exception {
        String body = "{\"sourceAccountNo\":\"12345678\",\"targetAccountNo\":\"87654321\",\"amount\":1}";
        when(ledger.exchange(eq(HttpMethod.POST), eq("/internal/v1/ledger/transfers"),
                eq(Map.of()), eq(body), eq("fixed-key"), any()))
                .thenReturn(ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON)
                        .body("{}".getBytes()));

        mvc.perform(post("/api/v1/transfers").header("Idempotency-Key", "fixed-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsUnsafePathVariableBeforeCallingLedger() throws Exception {
        mvc.perform(get("/api/v1/accounts/not-a-number"))
                .andExpect(status().isBadRequest());
    }
}
