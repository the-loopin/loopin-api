package com.loopin.api.common.ratelimit;

import com.loopin.api.config.RateLimitProperties;

public interface RateLimiterService {

    RateLimitResult tryConsume(String key, RateLimitProperties.Policy policy);
}
