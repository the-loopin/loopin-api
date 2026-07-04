package com.loopin.api.common.ratelimit;

import com.loopin.api.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded. Please try again later.";

    private final RateLimitProperties properties;
    private final RateLimiterService rateLimiterService;
    private final ClientIdentifierResolver clientIdentifierResolver;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Policy policy = findPolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildRateLimitKey(request, policy);
        RateLimitResult result = rateLimiterService.tryConsume(key, policy);

        if (!result.allowed()) {
            writeTooManyRequestsResponse(request, response, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitProperties.Policy findPolicy(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = getRequestPath(request);

        return properties.getPolicies().stream()
                .filter(policy -> policy.isEnabled() && policy.getRequests() > 0)
                .filter(policy -> policy.getWindow() != null && !policy.getWindow().isNegative() && !policy.getWindow().isZero())
                .filter(policy -> policy.getMethods() != null && policy.getMethods().stream()
                        .anyMatch(configuredMethod -> method.equals(configuredMethod.toUpperCase(Locale.ROOT))))
                .filter(policy -> policy.getPaths() != null && policy.getPaths().stream()
                        .anyMatch(pattern -> pathMatcher.match(pattern, path)))
                .findFirst()
                .orElse(null);
    }

    private String getRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    private String buildRateLimitKey(HttpServletRequest request, RateLimitProperties.Policy policy) {
        return policy.getName()
                + ":"
                + request.getMethod().toUpperCase(Locale.ROOT)
                + ":"
                + clientIdentifierResolver.resolve(request);
    }

    private void writeTooManyRequestsResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\",\"path\":\"%s\",\"fieldErrors\":null}",
                LocalDateTime.now(),
                escapeJson(RATE_LIMIT_EXCEEDED_MESSAGE),
                escapeJson(request.getRequestURI())
        );

        response.getWriter().write(json);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
