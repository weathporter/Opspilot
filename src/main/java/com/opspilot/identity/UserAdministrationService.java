package com.opspilot.identity;

import com.opspilot.audit.AuditEventService;
import com.opspilot.audit.AuditOutcome;
import com.opspilot.common.DuplicateResourceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** 用户治理应用服务：校验唯一性、哈希凭据、保存用户并写入审计证据。 */
@Service
public class UserAdministrationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final Clock clock;

    public UserAdministrationService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    /**
     * 在单一事务中完成用户创建和审计记录；任何一步失败都会整体回滚。
     * 唯一性既做友好预检查，也捕获数据库唯一键竞争，保证并发请求只有一个成功。
     */
    @Transactional
    public AppUserResponse create(
            CreateUserRequest request,
            Authentication administrator,
            HttpServletRequest servletRequest
    ) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByUsername(username)) {
            throw duplicateUsername();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser user = AppUser.create(
                username,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.roles(),
                now
        );
        try {
            appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "USERNAME_ALREADY_EXISTS",
                    "该用户名已存在",
                    exception
            );
        }

        auditEventService.record(
                "USER_CREATED",
                AuditOutcome.SUCCESS,
                administrator.getName(),
                "USER",
                username,
                "创建平台用户",
                servletRequest
        );
        return AppUserResponse.from(user);
    }

    /** 管理员读取用户清单；返回 DTO 而非实体，避免凭据字段被序列化。 */
    @Transactional(readOnly = true)
    public List<AppUserResponse> list() {
        return appUserRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AppUserResponse::from)
                .toList();
    }

    private static DuplicateResourceException duplicateUsername() {
        return new DuplicateResourceException("USERNAME_ALREADY_EXISTS", "该用户名已存在");
    }
}
