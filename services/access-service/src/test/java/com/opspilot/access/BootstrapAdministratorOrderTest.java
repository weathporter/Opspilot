package com.opspilot.access;

import com.opspilot.audit.AuditEventService;
import com.opspilot.identity.AppUserRepository;
import com.opspilot.identity.BootstrapAdministrator;
import com.opspilot.identity.BootstrapAdministratorProperties;
import com.opspilot.identity.BootstrapClaimRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 首次双副本启动前，必须先拿到 MySQL 串行化锁，再判断身份库是否为空。 */
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
