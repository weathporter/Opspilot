package com.opspilot.account;

import com.opspilot.common.DuplicateResourceException;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.dashboard.BusinessDataChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 账户模块的应用服务，负责编排开户、单条查询和列表查询用例。
 *
 * <p>Controller 只处理 HTTP，Account 实体维护余额和状态规则，本类则负责事务边界、
 * 仓储调用和响应模型转换。这是模块化单体中非常典型的应用层职责。</p>
 */
@Service
public class AccountApplicationService {

    /** 账户持久化端口，由 Spring Data JPA 在运行期生成实现。 */
    private final AccountRepository accountRepository;

    /** 可替换时间源使测试能够固定“当前时间”，避免直接调用系统时钟。 */
    private final Clock clock;

    /** 发布轻量业务变化事件，监听器只在数据库提交成功后清理总览缓存。 */
    private final ApplicationEventPublisher eventPublisher;

    public AccountApplicationService(
            AccountRepository accountRepository,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 执行开户事务。
     *
     * <p>{@code saveAndFlush} 立即把 INSERT 发送到 MySQL，使账号唯一约束冲突能在当前
     * try/catch 中被转换为稳定业务错误，而不是延迟到事务提交之后才抛出。</p>
     */
    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = Account.open(
                request.accountNo(),
                request.holderName().trim(),
                request.openingBalance(),
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS)
        );
        try {
            Account saved = accountRepository.saveAndFlush(account);
            // 事件在事务内发布，但 AFTER_COMMIT 监听器只会在本方法成功提交后执行。
            eventPublisher.publishEvent(new BusinessDataChangedEvent(
                    BusinessDataChangedEvent.ChangeType.ACCOUNT_CREATED
            ));
            return AccountResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            // 数据库唯一约束是并发场景下的最终防线，比“先查再插”更可靠。
            throw new DuplicateResourceException("ACCOUNT_ALREADY_EXISTS", "账号已存在", exception);
        }
    }

    /**
     * 查询管理控制台所需的账户列表。
     *
     * <p>只读事务关闭不必要的脏检查；结果先在事务内转换为 DTO，防止把 JPA 实体和
     * 持久化上下文泄露到 Web 层。限制范围 1～100 是服务端保护，不能只依赖前端自觉。</p>
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> list(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        PageRequest page = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = query == null ? "" : query.trim();

        List<Account> accounts = keyword.isEmpty()
                ? accountRepository.findAll(page).getContent()
                : accountRepository.findByAccountNoContainingOrHolderNameContainingIgnoreCase(
                        keyword,
                        keyword,
                        page
                );
        return accounts.stream().map(AccountResponse::from).toList();
    }

    /** 按业务账号查询账户；不存在时统一转换为 HTTP 404。 */
    @Transactional(readOnly = true)
    public AccountResponse get(String accountNo) {
        return accountRepository.findByAccountNo(accountNo)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("ACCOUNT_NOT_FOUND", "账户不存在"));
    }
}
