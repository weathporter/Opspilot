package com.opspilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

/** 运维读模型进程；没有账户和转账写权限，也不连接账务数据库。 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OperationsApplication {
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    public static void main(String[] args) {
        SpringApplication.run(OperationsApplication.class, args);
    }
}
