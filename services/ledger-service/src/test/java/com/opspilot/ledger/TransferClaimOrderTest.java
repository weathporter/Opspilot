package com.opspilot.ledger;

import com.opspilot.account.AccountRepository;
import com.opspilot.transfer.CreateTransferRequest;
import com.opspilot.transfer.LedgerEntryRepository;
import com.opspilot.transfer.TransferApplicationService;
import com.opspilot.transfer.TransferOrder;
import com.opspilot.transfer.TransferOrderRepository;
import com.opspilot.transfer.TransferRequestClaimRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 同键重试必须先获得数据库中的键级串行化，再读取旧订单。 */
class TransferClaimOrderTest {
    @Test
    void acquiresKeyClaimBeforeReadingExistingOrder() {
        AccountRepository accounts = mock(AccountRepository.class);
        TransferOrderRepository orders = mock(TransferOrderRepository.class);
        LedgerEntryRepository entries = mock(LedgerEntryRepository.class);
        TransferRequestClaimRepository claims = mock(TransferRequestClaimRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        TransferOrder existing = TransferOrder.start("same-key", "12345678", "87654321",
                new BigDecimal("10.00"), LocalDateTime.of(2026, 9, 24, 12, 0));
        existing.complete(LocalDateTime.of(2026, 9, 24, 12, 0));
        when(orders.findByRequestId("same-key")).thenReturn(Optional.of(existing));

        new TransferApplicationService(accounts, orders, entries, claims, Clock.systemUTC(), events)
                .transfer("same-key", new CreateTransferRequest("12345678", "87654321",
                        new BigDecimal("10.00")));

        var sequence = inOrder(claims, orders);
        sequence.verify(claims).acquire("same-key");
        sequence.verify(orders).findByRequestId("same-key");
    }
}
