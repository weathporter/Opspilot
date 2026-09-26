package com.opspilot.operations;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 对账的执行、最近历史与单批次证据；HTTP 入口只接受 Access 的服务凭据。 */
@RestController
@RequestMapping("/internal/v1/operations/reconciliations")
public class ReconciliationController {
    private final ReconciliationApplicationService application;
    private final ReconciliationPersistenceService persistence;

    public ReconciliationController(ReconciliationApplicationService application,
                                    ReconciliationPersistenceService persistence) {
        this.application = application;
        this.persistence = persistence;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReconciliationRunResponse run(@RequestParam(defaultValue = "100") int limit) {
        return application.run(limit);
    }

    @GetMapping
    public List<ReconciliationRunResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        return persistence.recent(limit);
    }

    @GetMapping("/{id}")
    public ReconciliationDetailResponse detail(@PathVariable long id) {
        return persistence.detail(id);
    }
}
