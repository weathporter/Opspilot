package com.opspilot.access;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 保留浏览器已使用的 URL；RBAC 由 Access 的 SecurityConfiguration 在进入此处前执行。 */
@RestController
@RequestMapping("/api/v1/operations")
public class OperationsProxyController {
    private final OperationsHttpClient operations;

    public OperationsProxyController(OperationsHttpClient operations) {
        this.operations = operations;
    }

    @GetMapping("/summary")
    public ResponseEntity<byte[]> summary() {
        return operations.summary(MDC.get("traceId"));
    }
}
