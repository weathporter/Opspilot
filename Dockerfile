# 完整的多阶段构建：第一阶段负责编译，第二阶段只负责运行。
# 优点是构建机只需 Docker，不必预装 Maven/JDK；最终镜像也不会携带源码和 Maven 缓存。

# builder 阶段使用完整 JDK + Maven。
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace

# 先只复制 pom 并下载依赖。只要 pom.xml 没变，这一层就能命中 Docker 缓存，源码变化无需重下依赖。
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# 再复制源码并打包；测试由 CI/发布门禁单独执行，镜像构建阶段跳过以避免重复运行。
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# runtime 阶段只保留轻量 JRE，缩小镜像体积和攻击面。
FROM eclipse-temurin:17-jre-alpine

# 构建参数用于写入 OCI 镜像元数据，可通过 docker build --build-arg APP_VERSION=... 覆盖。
ARG APP_VERSION=dev
LABEL org.opencontainers.image.title="NorthLedger" \
      org.opencontainers.image.description="Cloud-native operations practice for a financial transaction service" \
      org.opencontainers.image.version="${APP_VERSION}"

WORKDIR /app

# 使用固定 UID/GID 的非 root 用户运行 Java；即使应用被利用，也减少对容器文件系统的权限。
RUN addgroup -S -g 10001 opspilot \
    && adduser -S -D -H -u 10001 -G opspilot opspilot

# 从 builder 阶段只复制最终 JAR，并在复制时设置归属，避免额外 chown 镜像层。
COPY --from=builder --chown=opspilot:opspilot /workspace/target/northledger-*.jar /app/app.jar

# MaxRAMPercentage 让 JVM 感知容器内存上限；OOM 时退出进程，交给 Docker 重启而不是带病运行。
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# 下面的命令及进程均以非 root 身份执行。
USER 10001:10001

# EXPOSE 只是镜像文档，不会自动映射端口；实际映射由 Compose ports 决定。
EXPOSE 18080

# Docker 定期访问 readiness；连续失败后标记 unhealthy，供 Nginx depends_on 和排障使用。
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:18080/actuator/health/readiness || exit 1

# exec 形式让 Java 成为 PID 1，能直接收到 docker stop 发送的 SIGTERM 并执行优雅停机。
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
