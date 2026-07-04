package com.loopin.api.common.ratelimit;

import com.loopin.api.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded. Please try again later.";
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private final RateLimitProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong();

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

        long now = clock.millis();
        cleanupExpiredCounters(now);

        String key = buildRateLimitKey(request, policy);
        RequestCounter counter = incrementCounter(key, policy, now);

        if (counter.requestCount() > policy.getRequests()) {
            writeTooManyRequestsResponse(request, response);
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
                .filter(policy -> policy.getMethods().stream().anyMatch(configuredMethod -> method.equals(configuredMethod.toUpperCase(Locale.ROOT))))
                .filter(policy -> policy.getPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path)))
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
                + resolveClientIdentifier(request);
    }

    private String resolveClientIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "user:" + authentication.getName();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private RequestCounter incrementCounter(String key, RateLimitProperties.Policy policy, long now) {
        AtomicReference<RequestCounter> updatedCounter = new AtomicReference<>();

        counters.compute(key, (counterKey, existingCounter) -> {
            if (existingCounter == null || existingCounter.expiresAtMillis() <= now) {
                RequestCounter newCounter = new RequestCounter(now + policy.getWindow().toMillis(), 1);
                updatedCounter.set(newCounter);
                return newCounter;
            }

            RequestCounter incrementedCounter = existingCounter.increment();
            updatedCounter.set(incrementedCounter);
            return incrementedCounter;
        });

        return updatedCounter.get();
    }

    private void cleanupExpiredCounters(long now) {
        long previousCleanup = lastCleanupAt.get();
        if (now - previousCleanup < CLEANUP_INTERVAL_MILLIS || !lastCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }

        counters.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private void writeTooManyRequestsResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

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

    private record RequestCounter(long expiresAtMillis, int requestCount) {

        private RequestCounter increment() {
            return new RequestCounter(expiresAtMillis, requestCount + 1);
        }
    }
}
