package com.opspilot.ledger;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** 内部服务凭据是账务 API 的第二道边界；NetworkPolicy 失效时仍不能匿名转账。 */
class InternalServiceCredentialFilterTest {

    private static final String ACCESS_SECRET = "local-access-secret-0123456789";
    private static final String OPERATIONS_SECRET = "local-operations-secret-0123456789";

    @Test
    void rejectsMissingOrIncorrectBearerBeforeController() throws ServletException, IOException {
        InternalServiceCredentialFilter filter = new InternalServiceCredentialFilter(
                ACCESS_SECRET, OPERATIONS_SECRET);

        MockHttpServletRequest missing = internalRequest();
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        MockFilterChain missingChain = new MockFilterChain();
        filter.doFilter(missing, missingResponse, missingChain);
        assertThat(missingResponse.getStatus()).isEqualTo(401);
        assertThat(missingChain.getRequest()).isNull();

        MockHttpServletRequest incorrect = internalRequest();
        incorrect.addHeader("Authorization", "Bearer wrong-token");
        MockHttpServletResponse incorrectResponse = new MockHttpServletResponse();
        MockFilterChain incorrectChain = new MockFilterChain();
        filter.doFilter(incorrect, incorrectResponse, incorrectChain);
        assertThat(incorrectResponse.getStatus()).isEqualTo(401);
        assertThat(incorrectChain.getRequest()).isNull();
    }

    @Test
    void acceptsExactCredentialOnlyForInternalPath() throws ServletException, IOException {
        InternalServiceCredentialFilter filter = new InternalServiceCredentialFilter(
                ACCESS_SECRET, OPERATIONS_SECRET);
        MockHttpServletRequest request = internalRequest();
        request.addHeader("Authorization", "Bearer " + ACCESS_SECRET);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void rejectsIdenticalReadAndWriteSecretsAtStartup() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new InternalServiceCredentialFilter(ACCESS_SECRET, ACCESS_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MockHttpServletRequest internalRequest() {
        return new MockHttpServletRequest("POST", "/internal/v1/ledger/transfers");
    }
}
