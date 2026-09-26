package com.opspilot.operations;

/** Operations 依赖的最小账务读契约；测试可以替换为确定性的假实现。 */
public interface LedgerStatisticsGateway {
    String snapshot();

    /** 最多 100 笔最近订单与双边流水；底层仍由 Ledger 负责数据归属与校验。 */
    String reconciliationCandidates(int limit);
}
