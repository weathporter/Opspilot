package com.opspilot.identity;

import com.opspilot.audit.AuditEventService;
import com.opspilot.audit.AuditOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * 在全新数据库中按外部环境变量创建一次管理员。
 *
 * <p>该机制解决“系统第一次由谁创建管理员”的鸡生蛋问题，同时坚持三个边界：不硬编码
 * 凭据、不记录密码、不在已有用户时静默改密。</p>
 */
@Component
public class BootstrapAdministrator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapAdministrator.class);

    private final BootstrapAdministratorProperties properties;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final Clock clock;

    public BootstrapAdministrator(
            BootstrapAdministratorProperties properties,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            Clock clock
    ) {
        this.properties = properties;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    /** 校验配置完整性，并在事务中原子创建初始管理员。 */
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        boolean hasUsername = StringUtils.hasText(properties.username());
        boolean hasPassword = StringUtils.hasText(properties.password());

        // 完全未配置表示本次启动不执行引导，这是生产环境正常稳态。
        if (!hasUsername && !hasPassword) {
            return;
        }
        if (!hasUsername || !hasPassword) {
            throw new IllegalStateException("引导管理员用户名与密码必须同时配置");
        }
        if (properties.password().length() < 12) {
            throw new IllegalStateException("引导管理员密码至少需要 12 个字符");
        }

        String username = properties.username().trim().toLowerCase(Locale.ROOT);
        // 引导只允许初始化空身份库；库中已有任何用户后，改环境变量也不能借此追加高权限账号。
        if (appUserRepository.count() > 0) {
            logger.atInfo()
                    .addKeyValue("event", "bootstrap_administrator_skipped")
                    .addKeyValue("reason", "identity_store_initialized")
                    .log("initial administrator bootstrap skipped");
            return;
        }

        String displayName = StringUtils.hasText(properties.displayName())
                ? properties.displayName().trim()
                : "System Administrator";
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser administrator = AppUser.create(
                username,
                passwordEncoder.encode(properties.password()),
                displayName,
                Set.of(UserRole.ADMIN),
                now
        );
        appUserRepository.save(administrator);

        // 管理员与审计事件共用本事务；任一写入失败都会回滚，避免出现无法追溯的高权限身份。
        auditEventService.record(
                "BOOTSTRAP_ADMIN_CREATED",
                AuditOutcome.SUCCESS,
                username,
                "USER",
                username,
                "创建首个平台管理员",
                null
        );

        logger.atInfo()
                .addKeyValue("event", "bootstrap_administrator_created")
                .addKeyValue("username", username)
                .log("initial administrator created");
    }
}
