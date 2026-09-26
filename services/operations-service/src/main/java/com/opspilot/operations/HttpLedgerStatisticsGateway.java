package com.opspilot.operations;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 唯一固定的 Ledger 出站目标；Operations 只拥有 GET 统计权限。 */
public final class HttpLedgerStatisticsGateway implements LedgerStatisticsGateway {
    private final RestClient client;
    private final String token;

    public HttpLedgerStatisticsGateway(RestClient client, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Ledger read token must not be empty");
        }
        this.client = client;
        this.token = token;
    }

    @Override
    public String snapshot() {
        try {
            return client.get().uri("/internal/v1/ledger/statistics/snapshot")
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        String traceId = MDC.get("traceId");
                        if (traceId != null) {
                            headers.set("X-Request-ID", traceId);
                        }
                    }).retrieve().body(String.class);
        } catch (RestClientException exception) {
            throw new DownstreamUnavailableException("Ledger statistics request failed", exception);
        }
    }

    @Override
    public String reconciliationCandidates(int limit) {
        try {
            return client.get().uri(uri -> uri.path("/internal/v1/ledger/statistics/reconciliation-candidates")
                            .queryParam("limit", limit).build())
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        String traceId = MDC.get("traceId");
                        if (traceId != null) {
                            headers.set("X-Request-ID", traceId);
                        }
                    }).retrieve().body(String.class);
        } catch (RestClientException exception) {
            throw new DownstreamUnavailableException("Ledger reconciliation request failed", exception);
        }
    }
}
