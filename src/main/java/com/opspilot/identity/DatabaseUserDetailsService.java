package com.opspilot.identity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 把平台用户模型适配为 Spring Security 的 {@link UserDetailsService}。
 *
 * <p>异常信息保持笼统，避免攻击者通过登录响应枚举“哪些用户名真实存在”。</p>
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** 根据小写登录名加载密码哈希、账号状态和固定集合角色。 */
    @Override
    public UserDetails loadUserByUsername(String rawUsername) throws UsernameNotFoundException {
        String username = rawUsername == null ? "" : rawUsername.trim().toLowerCase(Locale.ROOT);
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));

        String[] authorities = appUser.getRoles().stream()
                .map(UserRole::authority)
                .sorted()
                .toArray(String[]::new);

        return User.withUsername(appUser.getUsername())
                .password(appUser.passwordHashForAuthentication())
                .authorities(authorities)
                .disabled(appUser.getStatus() == UserStatus.DISABLED)
                .accountLocked(appUser.getStatus() == UserStatus.LOCKED)
                .build();
    }
}
