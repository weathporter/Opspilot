package com.opspilot.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 发起转账接口的 JSON 输入契约。
 * 格式校验在 HTTP 边界完成；“两个账号不能相同、余额是否足够”等跨字段/领域规则
 * 仍由应用服务和 Account 实体负责，二者职责不能混淆。
 *
 * @param sourceAccountNo 付款账号，必须是 8~32 位数字
 * @param targetAccountNo 收款账号，必须是 8~32 位数字
 * @param amount 转账金额，最小 0.01 且保留不超过两位小数
 */
public record CreateTransferRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{8,32}$", message = "付款账号必须为8至32位数字")
        String sourceAccountNo,

        @NotBlank
        @Pattern(regexp = "^[0-9]{8,32}$", message = "收款账号必须为8至32位数字")
        String targetAccountNo,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
