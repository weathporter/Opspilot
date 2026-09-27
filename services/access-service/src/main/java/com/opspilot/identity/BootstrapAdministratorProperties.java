package com.opspilot.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 首次部署引导管理员的外部配置。
 *
 * <p>所有值默认为空，仓库不会包含可登录的生产账号。部署者只在首次启动时通过环境变量
 * 注入，创建成功后即可撤掉这些变量；后续启动绝不会覆盖已有密码。</p>
 */
@ConfigurationProperties(prefix = "northledger.security.bootstrap")
public record BootstrapAdministratorProperties(
        String username,
        String password,
        String displayName
) {
}
