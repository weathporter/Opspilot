package com.opspilot.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** 管理员创建用户的输入边界；密码只在本次请求内存在，进入服务后立即哈希。 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,64}", message = "用户名只能包含字母、数字、点、下划线和连字符，长度 3-64")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 12, max = 128, message = "密码长度必须为 12-128 个字符")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 64, message = "显示名称不能超过 64 个字符")
        String displayName,

        @NotEmpty(message = "至少选择一个角色")
        Set<UserRole> roles
) {
}
