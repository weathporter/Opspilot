package com.opspilot.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 运行总览 HTTP 入口，向前端提供单一且一致的聚合读模型。 */
@RestController
@RequestMapping("/api/v1/operations")
public class OperationsDashboardController {

    private final OperationsDashboardApplicationService dashboardApplicationService;

    public OperationsDashboardController(OperationsDashboardApplicationService dashboardApplicationService) {
        this.dashboardApplicationService = dashboardApplicationService;
    }

    /** GET /api/v1/operations/summary：生成当前运行与业务数据快照。 */
    @GetMapping("/summary")
    public OperationsSummaryResponse summary() {
        return dashboardApplicationService.getSummary();
    }
}
