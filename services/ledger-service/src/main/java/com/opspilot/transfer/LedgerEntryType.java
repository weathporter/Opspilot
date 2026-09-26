package com.opspilot.transfer;

/** 资金流水方向；金额字段保持正数，由方向表达余额是减少还是增加。 */
public enum LedgerEntryType {
    /** 借记：资金从账户转出，余额减少。 */
    DEBIT,

    /** 贷记：资金进入账户，余额增加。 */
    CREDIT
}
