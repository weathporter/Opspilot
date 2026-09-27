# syntax=docker/dockerfile:1
# 三个服务各自独立构建镜像；不把整个原单体打包进 Operations。
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY services/operations-service/pom.xml ./pom.xml
COPY services/operations-service/src ./src
# 测试由 CI 先验证；镜像构建复用 Maven 下载缓存并仅编译生产源码。
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
ARG APP_VERSION=dev
LABEL org.opencontainers.image.title="NorthLedger Operations Service" \
      org.opencontainers.image.version="${APP_VERSION}"
WORKDIR /app
RUN addgroup -S -g 10001 northledger && adduser -S -D -H -u 10001 -G northledger northledger
COPY --from=builder --chown=10001:10001 /workspace/target/northledger-operations-service-*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 18082
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:18082/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
