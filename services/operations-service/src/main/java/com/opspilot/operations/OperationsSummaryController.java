package com.opspilot.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 只供 Access 调用的运行总览入口；公网 Ingress 不暴露内部路径。 */
@RestController
@RequestMapping("/internal/v1/operations")
public class OperationsSummaryController {
    private final OperationsSummaryService summary;

    public OperationsSummaryController(OperationsSummaryService summary) {
        this.summary = summary;
    }

    @GetMapping("/summary")
    public JsonNode summary() {
        return summary.summary();
    }
}
