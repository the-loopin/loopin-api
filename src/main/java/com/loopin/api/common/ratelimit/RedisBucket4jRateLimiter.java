package com.loopin.api.common.ratelimit;

import com.loopin.api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "rate-limit.storage", havingValue = "redis")
public class RedisBucket4jRateLimiter implements RateLimiterService {

    private final RateLimitProperties properties;
    private final ProxyManager<byte[]> proxyManager;

    public RedisBucket4jRateLimiter(
            RateLimitProperties properties,
            LettuceConnectionFactory lettuceConnectionFactory
    ) {
        this.properties = properties;
        AbstractRedisClient client = lettuceConnectionFactory.getRequiredNativeClient();

        if (client instanceof RedisClient redisClient) {
            this.proxyManager = Bucket4jLettuce.casBasedBuilder(redisClient).build();
        } else if (client instanceof RedisClusterClient redisClusterClient) {
            this.proxyManager = Bucket4jLettuce.casBasedBuilder(redisClusterClient).build();
        } else {
            throw new IllegalStateException("Unsupported Redis client type for Bucket4j: " + client.getClass().getName());
        }
    }

    @Override
    public RateLimitResult tryConsume(String key, RateLimitProperties.Policy policy) {
        ConsumptionProbe probe = proxyManager
                .getProxy(redisKey(key), () -> buildConfiguration(policy))
                .tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                probe.getRemainingTokens(),
                toRetryAfterSeconds(probe.getNanosToWaitForRefill())
        );
    }

    private byte[] redisKey(String key) {
        return (properties.getKeyPrefix() + ":" + key).getBytes(StandardCharsets.UTF_8);
    }

    private BucketConfiguration buildConfiguration(RateLimitProperties.Policy policy) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.getRequests())
                        .refillGreedy(policy.getRequests(), policy.getWindow())
                        .build())
                .build();
    }

    private long toRetryAfterSeconds(long nanosToWait) {
        if (nanosToWait <= 0) {
            return 0;
        }
        return Math.max(1, (long) Math.ceil(nanosToWait / 1_000_000_000.0));
    }
}
