package com.loopin.api.common.ratelimit;

import com.loopin.api.common.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIdentifierResolver {

    private final RateLimitProperties properties;

    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return "user:" + authentication.getName();
        }

        return "ip:" + resolveIpAddress(request);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (isTrustedProxy(request.getRemoteAddr())) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (properties.getTrustedProxies() == null) {
            return false;
        }

        return properties.getTrustedProxies().stream()
                .filter(proxy -> proxy != null && !proxy.isBlank())
                .map(IpAddressMatcher::new)
                .anyMatch(matcher -> matcher.matches(remoteAddress));
    }
}
