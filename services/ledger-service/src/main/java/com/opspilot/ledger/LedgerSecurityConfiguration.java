package com.opspilot.ledger;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 账务服务不接受浏览器会话，只接受 Access 服务的内部凭据。
 *
 * <p>Spring Security 6.5 官方文档建议显式声明请求授权并用末尾 denyAll 收口未知路径；
 * 自定义 OncePerRequestFilter 通过 addFilterBefore 进入安全链，而不是注册成独立 Servlet Filter。
 * 参考：https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html
 * 以及：https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html</p>
 */
@Configuration
public class LedgerSecurityConfiguration {

    @Bean
    SecurityFilterChain ledgerSecurityFilterChain(
            HttpSecurity http,
            @Value("${northledger.internal.access-token}") String accessToken,
            @Value("${northledger.internal.operations-read-token}") String operationsReadToken
    ) throws Exception {
        http
                // 服务间 Bearer 凭据不依靠浏览器 Cookie，故不需要浏览器型 CSRF 与 HttpSession。
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/internal/v1/ledger/statistics/**")
                        .hasAnyRole("ACCESS", "OPERATIONS_READ")
                        .requestMatchers("/internal/v1/ledger/**").hasRole("ACCESS")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(new InternalServiceCredentialFilter(accessToken, operationsReadToken),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
