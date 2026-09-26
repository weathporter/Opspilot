package com.opspilot.access;

import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 兼容现有前端的账户与转账 URL，但不在 Access 保存账户或资金数据。
 * 登录、CSRF 和角色检查先由 SecurityConfiguration 完成，随后此适配器才转发到 Ledger。
 */
@RestController
@RequestMapping("/api/v1")
public class BusinessProxyController {
    private static final Pattern ACCOUNT_NO = Pattern.compile("[0-9]{8,32}");
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private final LedgerHttpClient ledger;

    public BusinessProxyController(LedgerHttpClient ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/accounts")
    public ResponseEntity<byte[]> accounts(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "50") String limit) {
        // queryParam 负责 URL 编码；浏览器无法在这里提供任意内部主机名。
        return ledger.exchange(HttpMethod.GET, "/internal/v1/ledger/accounts",
                Map.of("query", query, "limit", limit), null, null, traceId());
    }

    @PostMapping("/accounts")
    public ResponseEntity<byte[]> createAccount(@RequestBody String body) {
        return ledger.exchange(HttpMethod.POST, "/internal/v1/ledger/accounts",
                Map.of(), body, null, traceId());
    }

    @GetMapping("/accounts/{accountNo}")
    public ResponseEntity<byte[]> account(@PathVariable String accountNo) {
        if (!ACCOUNT_NO.matcher(accountNo).matches()) {
            return ResponseEntity.badRequest().build();
        }
        return ledger.exchange(HttpMethod.GET, "/internal/v1/ledger/accounts/" + accountNo,
                Map.of(), null, null, traceId());
    }

    @GetMapping("/transfers")
    public ResponseEntity<byte[]> transfers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") String limit) {
        Map<String, String> query = status == null
                ? Map.of("limit", limit)
                : Map.of("status", status, "limit", limit);
        return ledger.exchange(HttpMethod.GET, "/internal/v1/ledger/transfers",
                query, null, null, traceId());
    }

    @PostMapping("/transfers")
    public ResponseEntity<byte[]> createTransfer(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody String body) {
        // 幂等键原样传给 Ledger 的唯一事务边界；Access 绝不自行重试资金写请求。
        return ledger.exchange(HttpMethod.POST, "/internal/v1/ledger/transfers",
                Map.of(), body, key, traceId());
    }

    @GetMapping("/transfers/{requestId}")
    public ResponseEntity<byte[]> transfer(@PathVariable String requestId) {
        if (!REQUEST_ID.matcher(requestId).matches()) {
            return ResponseEntity.badRequest().build();
        }
        return ledger.exchange(HttpMethod.GET, "/internal/v1/ledger/transfers/" + requestId,
                Map.of(), null, null, traceId());
    }

    @GetMapping("/transfers/{requestId}/ledger")
    public ResponseEntity<byte[]> transferLedger(@PathVariable String requestId) {
        if (!REQUEST_ID.matcher(requestId).matches()) {
            return ResponseEntity.badRequest().build();
        }
        return ledger.exchange(HttpMethod.GET, "/internal/v1/ledger/transfers/" + requestId + "/ledger",
                Map.of(), null, null, traceId());
    }

    /** CorrelationIdFilter 已规范化此值；从 MDC 读取避免转发未经校验的原始请求头。 */
    private String traceId() {
        return MDC.get("traceId");
    }
}
