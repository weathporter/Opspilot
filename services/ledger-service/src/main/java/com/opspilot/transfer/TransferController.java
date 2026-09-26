package com.opspilot.transfer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 转账模块的 HTTP 入口，只负责协议映射，把事务和资金规则委托给应用服务。 */
@RestController
@RequestMapping("/internal/v1/ledger/transfers")
public class TransferController {

    /** 转账执行、查询和审计读模型的统一应用服务。 */
    private final TransferApplicationService transferApplicationService;

    public TransferController(TransferApplicationService transferApplicationService) {
        this.transferApplicationService = transferApplicationService;
    }

    /**
     * POST /api/v1/transfers：发起转账。
     *
     * <p>Idempotency-Key 属于交付语义，因此放在请求头。客户端超时重试时复用该键，
     * 服务端会返回原订单而不会再次扣款。</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(
            @RequestHeader("Idempotency-Key") String requestId,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return transferApplicationService.transfer(requestId, request);
    }

    /** GET /api/v1/transfers：读取最近转账，供控制台表格和状态筛选使用。 */
    @GetMapping
    public List<TransferResponse> list(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return transferApplicationService.list(status, limit);
    }

    /** GET /api/v1/transfers/{requestId}：在响应丢失后确认交易最终状态。 */
    @GetMapping("/{requestId}")
    public TransferResponse get(@PathVariable String requestId) {
        return transferApplicationService.get(requestId);
    }

    /**
     * GET /api/v1/transfers/{requestId}/ledger：返回订单与双边流水证据。
     * 该接口把“业务结果”和“可审计证据”连接起来，是完整闭环而非普通 CRUD 的关键。
     */
    @GetMapping("/{requestId}/ledger")
    public TransferAuditResponse audit(@PathVariable String requestId) {
        return transferApplicationService.audit(requestId);
    }
}
