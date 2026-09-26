package com.opspilot.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 账户模块的 HTTP 适配器。
 *
 * <p>Controller 只负责 URL、JSON、参数校验和 HTTP 状态码，不直接访问仓储，也不编写
 * 余额规则。这样既能让 Web 协议与业务规则解耦，也便于在面试中清楚说明分层边界。</p>
 */
@RestController
@RequestMapping("/internal/v1/ledger/accounts")
public class AccountController {

    /** 真正执行开户、单条查询和列表查询用例的应用服务。 */
    private final AccountApplicationService accountApplicationService;

    /** 构造器注入让依赖不可变，并保证 Controller 创建后立即处于可用状态。 */
    public AccountController(AccountApplicationService accountApplicationService) {
        this.accountApplicationService = accountApplicationService;
    }

    /**
     * POST /api/v1/accounts：创建账户。
     * {@code @Valid} 会在进入服务前校验账号、姓名和开户余额，成功时返回 201。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountApplicationService.create(request);
    }

    /**
     * GET /api/v1/accounts：为管理控制台提供最近账户列表和关键字搜索。
     *
     * <p>limit 在服务层再次收口到安全范围，防止调用方一次读取过多数据；query 为空时
     * 返回最近创建的账户，不为空时同时匹配账号和户名。</p>
     */
    @GetMapping
    public List<AccountResponse> list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return accountApplicationService.list(query, limit);
    }

    /** GET /api/v1/accounts/{accountNo}：按业务账号读取单个账户快照。 */
    @GetMapping("/{accountNo}")
    public AccountResponse get(@PathVariable String accountNo) {
        return accountApplicationService.get(accountNo);
    }
}
