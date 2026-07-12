package com.loopin.api.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissingAndReturnsItInTheResponse() throws Exception {
        MockHttpServletResponse response = invokeFilter(new MockHttpServletRequest());

        String requestId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(UUID.fromString(requestId)).isNotNull();
    }

    @Test
    void reusesAValidClientProvidedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "gateway-42:request_7");
        AtomicReference<String> requestIdSeenByHandler = new AtomicReference<>();

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            requestIdSeenByHandler.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(requestIdSeenByHandler).hasValue("gateway-42:request_7");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("gateway-42:request_7");
    }

    @Test
    void replacesAnUnsafeClientProvidedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "unsafe request id");

        MockHttpServletResponse response = invokeFilter(request);
        String requestId = response.getHeader(CorrelationIdFilter.HEADER_NAME);

        assertThat(requestId).isNotEqualTo("unsafe request id");
        assertThat(UUID.fromString(requestId)).isNotNull();
    }

    @Test
    void clearsMdcAfterRequestCompletion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<String> requestIdSeenByHandler = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) ->
            requestIdSeenByHandler.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(requestIdSeenByHandler.get()).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private MockHttpServletResponse invokeFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });
        return response;
    }
}
