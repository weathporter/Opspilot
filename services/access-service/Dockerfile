# syntax=docker/dockerfile:1
# 仓库根目录作为 build context，镜像仅复制 Access 模块。
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY services/access-service/pom.xml ./pom.xml
COPY services/access-service/src ./src
# CI 的独立测试作业先行；镜像阶段跳过测试源码编译，Maven 缓存避免重复拉依赖。
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
ARG APP_VERSION=dev
LABEL org.opencontainers.image.title="NorthLedger Access Service" \
      org.opencontainers.image.version="${APP_VERSION}"
WORKDIR /app
RUN addgroup -S -g 10001 northledger && adduser -S -D -H -u 10001 -G northledger northledger
COPY --from=builder --chown=10001:10001 /workspace/target/northledger-access-service-*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 18080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:18080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
