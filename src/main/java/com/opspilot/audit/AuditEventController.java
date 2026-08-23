package com.opspilot.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 审计员与管理员使用的只读 HTTP 入口；权限由 SecurityConfiguration 在到达此处前执行。 */
@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    /** GET /api/v1/audit/events：返回最近事件，不暴露更新或删除端点。 */
    @GetMapping
    public List<AuditEventResponse> list(@RequestParam(defaultValue = "100") int limit) {
        return auditEventService.list(limit);
    }
}
