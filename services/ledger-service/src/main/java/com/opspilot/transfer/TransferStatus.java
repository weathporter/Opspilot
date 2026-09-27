package com.opspilot.transfer;

/** 转账订单在当前第一阶段中的生命周期状态。 */
public enum TransferStatus {
    /** 已创建并处于事务处理过程中；正常提交后不会长期停留在此状态。 */
    PROCESSING,

    /** 余额变更和双边流水均已完成。 */
    COMPLETED
}
