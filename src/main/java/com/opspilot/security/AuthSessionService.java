package com.opspilot.security;

import com.opspilot.identity.AppUser;
import com.opspilot.identity.AppUserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/** 把 Spring Security 的内部认证对象转换为对前端安全、稳定的会话视图。 */
@Service
public class AuthSessionService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AppUserRepository appUserRepository;

    public AuthSessionService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * 认证信息可能来自数据库登录，也可能来自 MockMvc 测试身份，因此显示名查询允许降级。
     * 角色从已认证 SecurityContext 读取，确保响应展示的是本次请求真正生效的权限。
     */
    public AuthSessionResponse from(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return AuthSessionResponse.anonymous();
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .sorted()
                .toList();
        String displayName = appUserRepository.findByUsername(authentication.getName())
                .map(AppUser::getDisplayName)
                .orElse(authentication.getName());
        return new AuthSessionResponse(true, authentication.getName(), displayName, roles);
    }
}
