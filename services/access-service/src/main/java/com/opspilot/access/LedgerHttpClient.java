package com.opspilot.access;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Access 到 Ledger 的唯一 HTTP 出站边界。
 *
 * <p>目标主机由部署配置固定，调用方只能选择账务内部路径；浏览器提交的 URL、Host 和
 * Authorization 头都不能改变出站目标或服务凭据。使用 Spring Framework 6.2 的 RestClient
 * exchange 保留业务 4xx/5xx 状态和响应体，不把合法的 409/422 错误改写为 500。
 * 官方依据：https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html</p>
 */
public final class LedgerHttpClient {

    private static final String INTERNAL_PREFIX = "/internal/v1/ledger/";
    private final RestClient restClient;
    private final String serviceCredential;

    public LedgerHttpClient(RestClient restClient, String serviceCredential) {
        if (serviceCredential == null || serviceCredential.isBlank()) {
            throw new IllegalArgumentException("INTERNAL_SERVICE_TOKEN must not be empty");
        }
        this.restClient = restClient;
        this.serviceCredential = serviceCredential;
    }

    /**
     * 传递一个已由 Access 完成会话认证和 RBAC 的请求。
     *
     * @param path 必须是代码构造的 Ledger 内部路径，不能由浏览器作为完整 URL 提供
     * @param query 经过 Controller 类型化解析的查询参数
     * @param body 已经通过外部参数校验的 JSON，可为 null
     * @param idempotencyKey 转账写入时保持客户端同一个键，其余请求为 null
     * @param requestId 统一请求追踪号，由 Access 入口过滤器规范化
     */
    public ResponseEntity<byte[]> exchange(
            HttpMethod method,
            String path,
            Map<String, String> query,
            String body,
            String idempotencyKey,
            String requestId
    ) {
        if (path == null || !path.startsWith(INTERNAL_PREFIX)
                || path.contains("..") || path.contains("//") || path.contains(":")
                || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("Invalid Ledger internal path");
        }

        RestClient.RequestBodySpec request = restClient.method(method)
                .uri(uri -> {
                    uri.path(path);
                    query.forEach(uri::queryParam);
                    return uri.build();
                })
                .headers(headers -> {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + serviceCredential);
                    if (requestId != null && !requestId.isBlank()) {
                        headers.set("X-Request-ID", requestId);
                    }
                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                        headers.set("Idempotency-Key", idempotencyKey);
                    }
                });
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }

        try {
            return request.exchange((outbound, inbound) -> {
                HttpHeaders safeHeaders = new HttpHeaders();
                if (inbound.getHeaders().getContentType() != null) {
                    safeHeaders.setContentType(inbound.getHeaders().getContentType());
                }
                // 不转发下游 Set-Cookie、Authorization 或内部主机头；浏览器只能接收 Access 的会话。
                return new ResponseEntity<>(inbound.getBody().readAllBytes(), safeHeaders, inbound.getStatusCode());
            });
        } catch (ResourceAccessException exception) {
            // 连接拒绝、超时等网络故障是 503，不是余额为零或交易成功；外部异常处理器统一响应。
            throw new DownstreamUnavailableException("Ledger service is unreachable", exception);
        }
    }
}
