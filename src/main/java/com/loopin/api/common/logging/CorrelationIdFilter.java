package com.loopin.api.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Establishes a request-scoped identifier before the security filter chain runs.
 * Client values are accepted only when they are safe to return in an HTTP header
 * and to include in a structured log field.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern SAFE_REQUEST_ID =
        Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0," + (MAX_REQUEST_ID_LENGTH - 1) + "}");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String requestId = validRequestIdOrNull(request.getHeader(HEADER_NAME));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            restoreContext(previousContext);
        }
    }

    private void restoreContext(Map<String, String> previousContext) {
        if (previousContext == null || previousContext.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previousContext);
        }
    }

    private String validRequestIdOrNull(String requestId) {
        if (requestId == null || requestId.length() > MAX_REQUEST_ID_LENGTH) {
            return null;
        }
        return SAFE_REQUEST_ID.matcher(requestId).matches() ? requestId : null;
    }
}
