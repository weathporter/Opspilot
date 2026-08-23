package com.opspilot.identity;

import com.opspilot.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 首个管理员引导的数据库边界测试。 */
@SpringBootTest
@ActiveProfiles("test")
class BootstrapAdministratorIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private BootstrapAdministrator bootstrapAdministrator;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    /** 用户库一旦初始化，即使环境变量换了用户名也不能再借引导机制追加管理员。 */
    @Test
    void doesNotCreateAdministratorWhenAnyUserAlreadyExists() throws Exception {
        // 清掉应用启动时创建的测试管理员，改放一个普通用户来模拟已初始化身份库。
        appUserRepository.deleteAll();
        appUserRepository.saveAndFlush(AppUser.create(
                "existing-operator",
                passwordEncoder.encode("Existing-Operator-123!"),
                "Existing Operator",
                Set.of(UserRole.OPERATOR),
                LocalDateTime.now(clock)
        ));

        bootstrapAdministrator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(appUserRepository.findAll())
                .extracting(AppUser::getUsername)
                .containsExactly("existing-operator");
    }
}
