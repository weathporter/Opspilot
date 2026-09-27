package com.opspilot.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建账户接口的输入契约。
 *
 * <p>校验规则写在边界 DTO 上，让非法请求在进入事务和数据库之前快速失败；
 * 全局异常处理器会把校验结果统一转换为 HTTP 400 及字段级错误。</p>
 *
 * @param accountNo 8~32 位纯数字账号，便于与银行类账号格式对齐
 * @param holderName 非空且最长 64 字符的开户人姓名
 * @param openingBalance 非负、最多 17 位整数和 2 位小数的开户余额
 */
public record CreateAccountRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{8,32}$", message = "账号必须为8至32位数字")
        String accountNo,

        @NotBlank
        @Size(max = 64)
        String holderName,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal openingBalance
) {
}
