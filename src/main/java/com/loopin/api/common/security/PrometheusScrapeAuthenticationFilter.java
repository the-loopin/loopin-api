package com.loopin.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates Prometheus using a deployment-provided machine credential. This avoids relying
 * on a short-lived user JWT and deliberately does not log or expose the supplied secret.
 */
@Component
public class PrometheusScrapeAuthenticationFilter extends OncePerRequestFilter {

    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String TOKEN_HEADER = "X-Prometheus-Token";

    private final String scrapeToken;

    public PrometheusScrapeAuthenticationFilter(
        @Value("${loopin.observability.prometheus.scrape-token:}") String scrapeToken
    ) {
        this.scrapeToken = scrapeToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return !PROMETHEUS_PATH.equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String providedToken = request.getHeader(TOKEN_HEADER);
        if (isValid(providedToken)) {
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    "prometheus",
                    null,
                    List.of(new SimpleGrantedAuthority("SCOPE_PROMETHEUS"))
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValid(String providedToken) {
        return !scrapeToken.isBlank()
            && providedToken != null
            && MessageDigest.isEqual(
                scrapeToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
            );
    }
}
