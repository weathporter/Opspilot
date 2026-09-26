package com.opspilot.operations;

/** 明确区分不存在的对账批次与数据库/下游服务不可用。 */
public class ReconciliationNotFoundException extends RuntimeException {
    public ReconciliationNotFoundException(long id) {
        super("Reconciliation run not found: " + id);
    }
}
