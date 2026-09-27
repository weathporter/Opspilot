package com.opspilot.identity;

import com.opspilot.audit.AuditEventService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 旧 Helm Chart 也可能运行多个 API Pod，首个管理员引导同样需要数据库级串行化。 */
class BootstrapAdministratorOrderTest {
    @Test
    void claimsBootstrapBeforeCheckingExistingUsers() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        BootstrapClaimRepository claim = mock(BootstrapClaimRepository.class);
        when(users.count()).thenReturn(1L);

        BootstrapAdministrator bootstrap = new BootstrapAdministrator(
                new BootstrapAdministratorProperties("admin", "long-test-password", ""),
                users, claim, mock(PasswordEncoder.class), mock(AuditEventService.class), Clock.systemUTC());
        bootstrap.run(new DefaultApplicationArguments());

        InOrder order = inOrder(claim, users);
        order.verify(claim).acquire();
        order.verify(users).count();
    }
}
