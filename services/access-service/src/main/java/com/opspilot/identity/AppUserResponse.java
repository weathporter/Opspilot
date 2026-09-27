package com.opspilot.identity;

import java.time.LocalDateTime;
import java.util.List;

/** 用户管理对外响应；白名单字段中明确没有密码或密码哈希。 */
public record AppUserResponse(
        Long id,
        String username,
        String displayName,
        UserStatus status,
        List<UserRole> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 角色排序保证接口与测试输出稳定。 */
    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles().stream().sorted().toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
