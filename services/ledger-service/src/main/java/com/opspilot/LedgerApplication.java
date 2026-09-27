package com.opspilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 账务服务唯一启动入口。
 *
 * <p>本模块的类路径仅含账户、转账和通用错误处理，不装载原单体的身份/看板模块；
 * 因此独立构建的 JAR 只拥有自己的资金数据和内部 HTTP 接口。</p>
 */
// 内部服务没有用户名密码登录入口，禁用 Boot 的演示账号自动生成。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
