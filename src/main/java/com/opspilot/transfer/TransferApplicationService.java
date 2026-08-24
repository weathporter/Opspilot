package com.opspilot.transfer;

import com.opspilot.account.Account;
import com.opspilot.account.AccountRepository;
import com.opspilot.common.BusinessRuleException;
import com.opspilot.common.DuplicateResourceException;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.dashboard.BusinessDataChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 转账用例的核心编排服务，是本项目体现事务、并发控制和幂等性的关键代码。
 *
 * <p>一次成功转账必须原子完成四件事：付款方扣款、收款方入账、保存转账订单、保存两条流水。
 * {@code @Transactional} 把它们放入同一数据库事务，任一步失败都会整体回滚。</p>
 */
@Service
public class TransferApplicationService {

    /** 用于锁定并更新付款、收款账户。 */
    private final AccountRepository accountRepository;

    /** 保存订单，并通过 requestId 判断调用是否已经处理。 */
    private final TransferOrderRepository transferOrderRepository;

    /** 保存与查询可审计的借记、贷记流水。 */
    private final LedgerEntryRepository ledgerEntryRepository;

    /** 统一、可测试且能控制数据库精度的业务时间源。 */
    private final Clock clock;

    /** 只在新转账完成时发布变化，幂等重放不会重复清缓存。 */
    private final ApplicationEventPublisher eventPublisher;

    public TransferApplicationService(
            AccountRepository accountRepository,
            TransferOrderRepository transferOrderRepository,
            LedgerEntryRepository ledgerEntryRepository,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.transferOrderRepository = transferOrderRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 执行或重放一笔转账。
     *
     * <p>顺序是面试重点：校验幂等键 → 无锁查询历史订单 → 按固定顺序锁账户 →
     * 锁后再次检查幂等键 → 修改余额 → 保存订单和双边流水 → 标记完成。</p>
     */
    @Transactional
    public TransferResponse transfer(String requestId, CreateTransferRequest request) {
        validateRequestId(requestId);
        if (request.sourceAccountNo().equals(request.targetAccountNo())) {
            throw new BusinessRuleException("SAME_ACCOUNT_TRANSFER", "付款账号与收款账号不能相同");
        }

        // 第一次查询是网络重试的快速路径，大多数重放无需再次竞争账户锁。
        TransferOrder existing = transferOrderRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return replay(existing, request);
        }

        /*
         * 所有事务都按账号字典序从小到大加锁。若 A→B 锁 A 再锁 B，而 B→A 反向加锁，
         * 两个事务可能互相等待形成死锁；固定顺序能显著降低这种风险。
         */
        String firstAccountNo = request.sourceAccountNo().compareTo(request.targetAccountNo()) < 0
                ? request.sourceAccountNo()
                : request.targetAccountNo();
        String secondAccountNo = firstAccountNo.equals(request.sourceAccountNo())
                ? request.targetAccountNo()
                : request.sourceAccountNo();

        Account first = lockAccount(firstAccountNo);
        Account second = lockAccount(secondAccountNo);

        /*
         * 锁后双检关闭并发窗口：两个相同 requestId 的请求可能同时通过第一次查询，
         * 后获得账户锁的请求必须再次确认前一个事务是否已创建订单。
         */
        existing = transferOrderRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return replay(existing, request);
        }

        // 加锁按账号排序，下面重新映射回业务语义上的付款方与收款方。
        Account source = first.getAccountNo().equals(request.sourceAccountNo()) ? first : second;
        Account target = first.getAccountNo().equals(request.targetAccountNo()) ? first : second;

        // MySQL TIMESTAMP(6) 只保存微秒；主动截断避免 Java 纳秒值和回读值不相等。
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        // 余额不足或账户不可用会抛出运行时业务异常，从而回滚整个数据库事务。
        source.debit(request.amount(), now);
        target.credit(request.amount(), now);

        TransferOrder transferOrder = TransferOrder.start(
                requestId,
                request.sourceAccountNo(),
                request.targetAccountNo(),
                request.amount(),
                now
        );
        transferOrderRepository.save(transferOrder);

        // 双边记账与余额变更在同一事务提交，避免出现余额已变但审计证据缺失。
        ledgerEntryRepository.saveAll(List.of(
                LedgerEntry.debit(
                        transferOrder,
                        source.getAccountNo(),
                        request.amount(),
                        source.getBalance(),
                        now
                ),
                LedgerEntry.credit(
                        transferOrder,
                        target.getAccountNo(),
                        request.amount(),
                        target.getBalance(),
                        now
                )
        ));

        // JPA 脏检查会在提交前自动生成账户余额和订单状态的 UPDATE。
        transferOrder.complete(now);
        // 前面的两个 replay 快速路径已经返回，因此这里只代表一笔新完成的转账。
        eventPublisher.publishEvent(new BusinessDataChangedEvent(
                BusinessDataChangedEvent.ChangeType.TRANSFER_COMPLETED
        ));
        return TransferResponse.from(transferOrder);
    }

    /**
     * 返回最近转账列表；状态为空表示全部。
     * limit 被收口到 1～100，既适合控制台，也避免无界查询拖慢数据库和网络。
     */
    @Transactional(readOnly = true)
    public List<TransferResponse> list(TransferStatus status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageRequest page = PageRequest.of(0, safeLimit);
        List<TransferOrder> orders = status == null
                ? transferOrderRepository.findAllByOrderByCreatedAtDesc(page)
                : transferOrderRepository.findAllByStatusOrderByCreatedAtDesc(status, page);
        return orders.stream().map(TransferResponse::from).toList();
    }

    /** 按幂等键查询转账结果，可用于客户端在响应丢失后确认最终状态。 */
    @Transactional(readOnly = true)
    public TransferResponse get(String requestId) {
        return transferOrderRepository.findByRequestId(requestId)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("TRANSFER_NOT_FOUND", "转账记录不存在"));
    }

    /**
     * 查询订单及其双边流水，并在服务端计算是否借贷平衡。
     * 所有实体都在只读事务内转换为 DTO，返回后不依赖 Hibernate 会话。
     */
    @Transactional(readOnly = true)
    public TransferAuditResponse audit(String requestId) {
        TransferOrder order = transferOrderRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TRANSFER_NOT_FOUND", "转账记录不存在"));
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferOrderRequestIdOrderByIdAsc(requestId);
        return TransferAuditResponse.of(order, entries);
    }

    /** 查询并悲观锁定账户，锁会一直持有到外层事务提交或回滚。 */
    private Account lockAccount(String accountNo) {
        return accountRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("ACCOUNT_NOT_FOUND", "账户不存在: " + accountNo));
    }

    /** 相同键和相同载荷返回旧结果；相同键配不同载荷则明确拒绝。 */
    private TransferResponse replay(TransferOrder existing, CreateTransferRequest request) {
        if (!existing.hasSamePayload(
                request.sourceAccountNo(),
                request.targetAccountNo(),
                request.amount()
        )) {
            throw new DuplicateResourceException(
                    "IDEMPOTENCY_KEY_CONFLICT",
                    "相同幂等键不能用于不同的转账请求"
            );
        }
        return TransferResponse.from(existing);
    }

    /** 幂等键的长度必须与数据库 VARCHAR(64) 契约一致。 */
    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new BusinessRuleException("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key长度必须为1至64个字符");
        }
    }
}
