package com.loopin.api;

import com.loopin.api.common.config.RateLimitProperties;
import com.loopin.api.common.ratelimit.RateLimiterService;
import com.loopin.api.common.ratelimit.RedisBucket4jRateLimiter;
import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "rate-limit.enabled=true",
        "rate-limit.storage=redis",
        "rate-limit.key-prefix=loopin:integration-rate-limit",
        "rate-limit.policies[0].name=auth",
        "rate-limit.policies[0].requests=1",
        "rate-limit.policies[0].window=1m",
        "rate-limit.policies[0].methods[0]=POST",
        "rate-limit.policies[0].paths[0]=/v1/auth/**"
})
class RedisIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RateLimiterService rateLimiterService;
    @Autowired private RateLimitProperties rateLimitProperties;
    @Autowired private LettuceConnectionFactory lettuceConnectionFactory;
    @Autowired private RedisConnectionFactory redisConnectionFactory;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }

    @Test
    void redisRateLimitIsSharedAcrossIndependentLimiterInstances() {
        RateLimitProperties.Policy policy = rateLimitProperties.getPolicies().getFirst();
        String key = "client-" + UUID.randomUUID();
        RedisBucket4jRateLimiter secondApplicationInstance = new RedisBucket4jRateLimiter(
                rateLimitProperties, lettuceConnectionFactory
        );

        assertThat(rateLimiterService).isInstanceOf(RedisBucket4jRateLimiter.class);
        assertThat(rateLimiterService.tryConsume(key, policy).allowed()).isTrue();
        assertThat(secondApplicationInstance.tryConsume(key, policy).allowed()).isFalse();
    }

    @Test
    void redisPubSubDeliversRealtimeMessageToSubscriber() throws Exception {
        String channel = "chat-realtime-" + UUID.randomUUID();
        String payload = "group-message";
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> actualPayload = new AtomicReference<>();
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(redisConnectionFactory);
        listenerContainer.addMessageListener((Message message, byte[] pattern) -> {
            actualPayload.set(new String(message.getBody(), StandardCharsets.UTF_8));
            received.countDown();
        }, new ChannelTopic(channel));
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        try {
            boolean delivered = false;
            for (int attempt = 0; attempt < 5 && !delivered; attempt++) {
                redisTemplate.convertAndSend(channel, payload);
                delivered = received.await(500, TimeUnit.MILLISECONDS);
            }

            assertThat(delivered).isTrue();
            assertThat(actualPayload).hasValue(payload);
        } finally {
            listenerContainer.stop();
            listenerContainer.destroy();
        }
    }
}
