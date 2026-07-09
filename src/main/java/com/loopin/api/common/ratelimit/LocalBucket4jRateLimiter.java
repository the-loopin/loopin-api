package com.loopin.api.common.ratelimit;

import com.loopin.api.common.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "rate-limit.storage", havingValue = "local", matchIfMissing = true)
public class LocalBucket4jRateLimiter implements RateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, RateLimitProperties.Policy policy) {
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> Bucket.builder()
                .addLimit(buildLimit(policy))
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return new RateLimitResult(
                probe.isConsumed(),
                probe.getRemainingTokens(),
                toRetryAfterSeconds(probe.getNanosToWaitForRefill())
        );
    }

    private Bandwidth buildLimit(RateLimitProperties.Policy policy) {
        return Bandwidth.builder()
                .capacity(policy.getRequests())
                .refillGreedy(policy.getRequests(), policy.getWindow())
                .build();
    }

    private long toRetryAfterSeconds(long nanosToWait) {
        if (nanosToWait <= 0) {
            return 0;
        }
        return Math.max(1, (long) Math.ceil(nanosToWait / 1_000_000_000.0));
    }
}
