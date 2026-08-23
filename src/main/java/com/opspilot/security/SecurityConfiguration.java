package com.opspilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * NorthLedger 的服务端安全边界。
 *
 * <p>规则顺序从最具体到最一般，Spring Security 只采用第一条匹配规则。前端隐藏菜单只是
 * 使用体验，真正的权限边界全部在这里执行。</p>
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/authorize-http-requests.html">请求授权官方文档</a>
 */
@Configuration
public class SecurityConfiguration {

    /** BCrypt cost=12，在当前项目规模下兼顾离线破解成本与登录响应时间。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * XSRF-TOKEN 需要允许 React 读取，真正的身份 Cookie SESSION 仍保持 HttpOnly。
     * Cookie 路径设为根路径，使登录、业务 API 与注销请求使用同一个 Token。
     */
    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    /** 配置认证方式、角色矩阵、CSRF、统一错误和浏览器安全响应头。 */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            AuthSessionService authSessionService,
            SecurityProblemWriter problemWriter,
            SecurityAuditRecorder securityAuditRecorder,
            CookieCsrfTokenRepository csrfRepository
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Spring Boot 异常转发仍要进入统一错误出口，否则原始 401/403 可能被二次覆盖。
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 健康、版本、Prometheus 抓取和匿名会话状态属于明确的最小公开面。
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/api/v1/auth/session").permitAll()
                        // 登录必须允许匿名提交，但仍受下面的 CSRF 保护。
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/session").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/session").authenticated()
                        // 资金写操作只允许业务操作员或管理员。
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/**", "/api/v1/transfers/**")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        // 业务读模型允许管理员、操作员和只读审计员。
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/accounts/**",
                                "/api/v1/transfers/**",
                                "/api/v1/operations/**")
                        .hasAnyRole("ADMIN", "OPERATOR", "AUDITOR")
                        // 用户治理和审计证据采用不同最小角色集合。
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                        .requestMatchers("/api/**").authenticated()
                        // 后端不托管静态页面，未声明路径默认拒绝，避免新端点忘记加权限。
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/session")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            securityAuditRecorder.loginSucceeded(request, authentication.getName());
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getOutputStream(),
                                    authSessionService.from(authentication));
                        })
                        .failureHandler((request, response, exception) -> {
                            securityAuditRecorder.loginFailed(request, request.getParameter("username"));
                            problemWriter.write(
                                    response,
                                    HttpStatus.UNAUTHORIZED,
                                    "INVALID_CREDENTIALS",
                                    "用户名或密码错误");
                        }))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> problemWriter.write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED",
                                "请先登录后再访问该资源"))
                        .accessDeniedHandler((request, response, exception) -> {
                            String username = SecurityContextHolder.getContext().getAuthentication() == null
                                    ? null
                                    : SecurityContextHolder.getContext().getAuthentication().getName();
                            securityAuditRecorder.accessDenied(request, username);
                            problemWriter.write(
                                    response,
                                    HttpStatus.FORBIDDEN,
                                    "ACCESS_DENIED",
                                    "当前用户没有执行该操作的权限");
                        }))
                .sessionManagement(session -> session
                        // 登录后更换会话标识，阻止攻击者预先固定 SESSION ID。
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .frameOptions(frame -> frame.deny()));

        return http.build();
    }
}
