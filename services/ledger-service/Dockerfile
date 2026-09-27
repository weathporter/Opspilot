# syntax=docker/dockerfile:1
# 仅把独立账务模块复制到构建阶段，避免将原单体源码/其他服务密钥打进镜像。
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY services/ledger-service/pom.xml ./pom.xml
COPY services/ledger-service/src ./src
# 测试已在独立 CI 作业执行；此阶段只产可运行 JAR，共享下载缓存但不带入最终镜像。
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp package -Dmaven.test.skip=true

# 运行镜像只含 JRE 和 Ledger JAR；固定非 root UID 供 K8s securityContext 对齐。
FROM eclipse-temurin:17-jre-alpine
ARG APP_VERSION=dev
LABEL org.opencontainers.image.title="NorthLedger Ledger Service" \
      org.opencontainers.image.version="${APP_VERSION}"
WORKDIR /app
RUN addgroup -S -g 10001 northledger && adduser -S -D -H -u 10001 -G northledger northledger
COPY --from=builder --chown=10001:10001 /workspace/target/northledger-ledger-service-*.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 18081
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:18081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
