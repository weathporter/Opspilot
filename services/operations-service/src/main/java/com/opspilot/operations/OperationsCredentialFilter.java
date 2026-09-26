package com.opspilot.operations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Access 到 Operations 的单一服务凭据边界；浏览器 Cookie 在这里不具有任何身份效力。 */
public final class OperationsCredentialFilter extends OncePerRequestFilter {
    private final byte[] credential;

    public OperationsCredentialFilter(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Operations access token must not be empty");
        }
        credential = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/v1/operations/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || !MessageDigest.isEqual(
                credential, header.substring(7).getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken("access-service", null,
                List.of(new SimpleGrantedAuthority("ROLE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
