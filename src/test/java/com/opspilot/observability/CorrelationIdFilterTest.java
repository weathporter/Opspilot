package com.opspilot.observability;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CorrelationIdFilter 的快速单元测试。
 * 不启动 Spring 和 MySQL，只构造 Servlet 模拟对象验证请求头规则，因此反馈速度很快。
 */
class CorrelationIdFilterTest {

    /** 被测过滤器没有外部依赖，可直接 new，保持测试聚焦。 */
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    /**
     * 证明合法的上游 X-Request-ID 会原样写回响应。
     * 这保证请求经过网关、Nginx 和应用后仍可使用同一个关联标识检索全链路日志。
     */
    @Test
    void keepsAValidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "demo-request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("demo-request-001");
    }

    /**
     * 证明含空格的不安全 ID 不会直接进入日志，而会被 UUID 替换。
     * 若该测试失败，意味着日志注入防护或响应关联能力可能退化。
     */
    @Test
    void replacesAnUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "unsafe id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .matches("[0-9a-f-]{36}");
    }
}
