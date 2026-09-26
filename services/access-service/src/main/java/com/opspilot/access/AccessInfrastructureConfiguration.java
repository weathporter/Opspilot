package com.opspilot.access;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Access 的出站连接统一配置：固定目的地、短超时、凭据只来自运行时 Secret。 */
@Configuration
public class AccessInfrastructureConfiguration {
    @Bean
    LedgerHttpClient ledgerHttpClient(
            @Value("${northledger.ledger.base-url}") String baseUrl,
            @Value("${northledger.internal.access-token}") String token) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 连接与读取都设界限，账务服务故障时不能无限占用 Access 的请求线程。
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        RestClient client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        return new LedgerHttpClient(client, token);
    }

    @Bean
    OperationsHttpClient operationsHttpClient(
            @Value("${northledger.operations.base-url}") String baseUrl,
            @Value("${northledger.internal.operations-token}") String token) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return new OperationsHttpClient(RestClient.builder().baseUrl(baseUrl)
                .requestFactory(requestFactory).build(), token);
    }
}
