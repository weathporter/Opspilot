package com.opspilot.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 内部服务发现地址和凭据在部署时注入，浏览器不可决定目标。 */
@Configuration
public class OperationsInfrastructureConfiguration {
    @Bean
    LedgerStatisticsGateway ledgerStatisticsGateway(
            @Value("${northledger.ledger.base-url}") String baseUrl,
            @Value("${northledger.internal.ledger-read-token}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return new HttpLedgerStatisticsGateway(RestClient.builder().baseUrl(baseUrl)
                .requestFactory(factory).build(), token);
    }
}
