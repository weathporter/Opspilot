package com.opspilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * OpsPilot 进程的唯一启动入口。
 *
 * <p>{@link SpringBootApplication} 同时开启自动配置、组件扫描和 Java 配置：
 * Spring 会从 {@code com.opspilot} 根包向下发现 Controller、Service、Repository、
 * Filter 等组件，并根据 classpath 中的依赖组装 Web、JPA、Actuator 等基础设施。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OpsPilotApplication {

    /**
     * 把命令行参数交给 Spring Boot，创建应用上下文并启动内嵌 Tomcat。
     * 当数据库连接、Flyway 迁移或配置绑定失败时，异常会在这里向上终止进程，
     * systemd/Docker 便能通过非零退出码识别启动失败并执行重启策略。
     *
     * @param args JVM 进程收到的命令行参数，例如 {@code --spring.profiles.active=prod}
     */
    public static void main(String[] args) {
        SpringApplication.run(OpsPilotApplication.class, args);
    }
}
