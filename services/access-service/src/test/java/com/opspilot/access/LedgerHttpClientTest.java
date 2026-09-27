package com.opspilot.access;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 真实 RestClient 请求构造测试：验证认证头、追踪号与固定目标，而不是断言模拟器本身。 */
class LedgerHttpClientTest {

    @Test
    void forwardsOnlyToConfiguredLedgerServiceWithIdentityHeaders() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ledger.internal:18081");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LedgerHttpClient client = new LedgerHttpClient(builder.build(), "test-internal-token");
        server.expect(once(), request -> {
                    // 查询参数顺序不是 HTTP 契约，只检查主机、路径和值。
                    var uri = request.getURI();
                    var params = org.springframework.web.util.UriComponentsBuilder.fromUri(uri)
                            .build().getQueryParams();
                    assertThat(uri.getHost()).isEqualTo("ledger.internal");
                    assertThat(uri.getPort()).isEqualTo(18081);
                    assertThat(uri.getPath()).isEqualTo("/internal/v1/ledger/accounts");
                    assertThat(params.getFirst("query")).isEqualTo("abc");
                    assertThat(params.getFirst("limit")).isEqualTo("2");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-internal-token"))
                .andExpect(header("X-Request-ID", "trace-123"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var response = client.exchange(HttpMethod.GET, "/internal/v1/ledger/accounts",
                Map.of("query", "abc", "limit", "2"), null, null, "trace-123");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("[]");
        server.verify();
    }

    @Test
    void rejectsCallerChosenAbsoluteOrTraversalPathBeforeNetworkCall() {
        LedgerHttpClient client = new LedgerHttpClient(RestClient.create("http://ledger.internal:18081"),
                "test-internal-token");
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, "https://evil.example/steal",
                Map.of(), null, null, "trace-123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, "/internal/v1/ledger/../admin",
                Map.of(), null, null, "trace-123")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectionFailureIsReportedAsServiceUnavailableNotSuccessfulEmptyData() {
        ClientHttpRequestFactory unavailable = (uri, method) -> {
            throw new java.io.IOException("simulated connection refused");
        };
        LedgerHttpClient client = new LedgerHttpClient(RestClient.builder()
                .baseUrl("http://ledger.internal:18081").requestFactory(unavailable).build(),
                "test-internal-token");

        assertThatThrownBy(() -> client.exchange(HttpMethod.GET, "/internal/v1/ledger/accounts",
                Map.of(), null, null, "trace-123"))
                .isInstanceOf(DownstreamUnavailableException.class);
    }
}
