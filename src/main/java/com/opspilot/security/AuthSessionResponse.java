package com.opspilot.security;

import java.util.List;

/**
 * 前端查询登录态时使用的稳定响应契约。
 *
 * <p>匿名响应完全省略用户名和显示名；已登录响应只包含界面与路由授权需要的最小信息，
 * 不返回密码哈希、数据库主键或 SESSION 标识。</p>
 */
public record AuthSessionResponse(
        boolean authenticated,
        String username,
        String displayName,
        List<String> roles
) {
    /** @return 不携带任何身份信息的匿名状态。 */
    public static AuthSessionResponse anonymous() {
        return new AuthSessionResponse(false, null, null, List.of());
    }
}
