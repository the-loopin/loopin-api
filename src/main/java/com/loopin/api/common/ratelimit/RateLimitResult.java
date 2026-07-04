package com.loopin.api.common.ratelimit;

public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {
}
