package com.opspilot.access;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Access 到 Operations 的固定只读调用；没有任意 URL、HTTP 方法或浏览器令牌透传入口。 */
public final class OperationsHttpClient {
    private final RestClient client;
    private final String token;

    public OperationsHttpClient(RestClient client, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Operations service token must not be empty");
        }
        this.client = client;
        this.token = token;
    }

    public ResponseEntity<byte[]> summary(String traceId) {
        return exchange(HttpMethod.GET, "/internal/v1/operations/summary", Map.of(), traceId);
    }

    public ResponseEntity<byte[]> runReconciliation(int limit, String traceId) {
        return exchange(HttpMethod.POST, "/internal/v1/operations/reconciliations",
                Map.of("limit", Integer.toString(limit)), traceId);
    }

    public ResponseEntity<byte[]> recentReconciliations(int limit, String traceId) {
        return exchange(HttpMethod.GET, "/internal/v1/operations/reconciliations",
                Map.of("limit", Integer.toString(limit)), traceId);
    }

    public ResponseEntity<byte[]> reconciliation(long id, String traceId) {
        return exchange(HttpMethod.GET, "/internal/v1/operations/reconciliations/" + id,
                Map.of(), traceId);
    }

    private ResponseEntity<byte[]> exchange(HttpMethod method, String path,
                                            Map<String, String> query, String traceId) {
        try {
            return client.method(method).uri(uri -> {
                        uri.path(path);
                        query.forEach(uri::queryParam);
                        return uri.build();
                    })
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                        if (traceId != null) {
                            headers.set("X-Request-ID", traceId);
                        }
                    }).exchange((request, response) -> {
                        HttpHeaders safe = new HttpHeaders();
                        if (response.getHeaders().getContentType() != null) {
                            safe.setContentType(response.getHeaders().getContentType());
                        }
                        // 只转发业务响应，不转发内部 Set-Cookie 或服务凭据。
                        return new ResponseEntity<>(response.getBody().readAllBytes(), safe, response.getStatusCode());
                    });
        } catch (ResourceAccessException exception) {
            throw new DownstreamUnavailableException("Operations service is unreachable", exception);
        }
    }
}
