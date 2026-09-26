package com.opspilot.access;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户通过 Access 的会话/CSRF/RBAC 执行对账，具体历史仍留在 Operations。 */
@RestController
@RequestMapping("/api/v1/operations/reconciliations")
public class ReconciliationProxyController {
    private final OperationsHttpClient operations;

    public ReconciliationProxyController(OperationsHttpClient operations) {
        this.operations = operations;
    }

    @PostMapping
    public ResponseEntity<byte[]> run(@RequestParam(defaultValue = "100") int limit) {
        if (limit < 1 || limit > 100) {
            return ResponseEntity.badRequest().build();
        }
        return operations.runReconciliation(limit, MDC.get("traceId"));
    }

    @GetMapping
    public ResponseEntity<byte[]> recent(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 50) {
            return ResponseEntity.badRequest().build();
        }
        return operations.recentReconciliations(limit, MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> detail(@PathVariable long id) {
        if (id < 1) {
            return ResponseEntity.badRequest().build();
        }
        return operations.reconciliation(id, MDC.get("traceId"));
    }
}
