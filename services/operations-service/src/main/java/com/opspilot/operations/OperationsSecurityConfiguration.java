package com.opspilot.operations;

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

/** Operations 是集群内部服务；仅接受 Access 的服务凭据并对未知路由默认拒绝。 */
@Configuration
public class OperationsSecurityConfiguration {
    @Bean
    SecurityFilterChain operationsSecurityFilterChain(HttpSecurity http,
            @Value("${northledger.internal.access-token}") String token) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**", "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/internal/v1/operations/**").hasRole("ACCESS")
                        .requestMatchers(HttpMethod.POST, "/internal/v1/operations/reconciliations")
                        .hasRole("ACCESS")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(new OperationsCredentialFilter(token), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
