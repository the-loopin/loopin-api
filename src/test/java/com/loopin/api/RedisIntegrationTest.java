package com.loopin.api;

import com.loopin.api.common.config.RateLimitProperties;
import com.loopin.api.common.ratelimit.RateLimiterService;
import com.loopin.api.common.ratelimit.RedisBucket4jRateLimiter;
import com.loopin.api.support.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.redis.cache.RedisCache;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopin.api.common.cache.CacheNames;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.listpublishedevents.CachedEventPage;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "rate-limit.enabled=true",
        "rate-limit.storage=redis",
        "rate-limit.key-prefix=loopin:integration-rate-limit",
        "rate-limit.policies[0].name=auth",
        "rate-limit.policies[0].requests=1",
        "rate-limit.policies[0].window=1m",
        "rate-limit.policies[0].methods[0]=POST",
        "rate-limit.policies[0].paths[0]=/v1/auth/**"
})
class RedisIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired private RateLimiterService rateLimiterService;
    @Autowired private RateLimitProperties rateLimitProperties;
    @Autowired private LettuceConnectionFactory lettuceConnectionFactory;
    @Autowired private RedisConnectionFactory redisConnectionFactory;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void clearRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
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
    void redisPubSubDeliversInfrastructureMessageToSubscriber() throws Exception {
        String channel = "integration-pubsub-" + UUID.randomUUID();
        String payload = "redis-message";
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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publishedEventsCache_canSerializeAndDeserializeCachedEventPage() {
        EventResponse event = new EventResponse();
        event.setId(UUID.randomUUID());
        event.setTitle("Cached Baku Event");
        event.setInterests(List.of());

        CachedEventPage original = new CachedEventPage(
            List.of(event),
            1
        );

        Cache cache = cacheManager.getCache(
            CacheNames.PUBLISHED_EVENTS
        );

        assertThat(cache)
            .as("Published events cache must exist")
            .isNotNull()
            .isInstanceOf(RedisCache.class);

        String cacheKey =
            "serialization-roundtrip-" + UUID.randomUUID();

        cache.put(cacheKey, original);

        Cache.ValueWrapper wrapper =
            cache.get(cacheKey);

        assertThat(wrapper)
            .as("Cache entry must be readable immediately after put")
            .isNotNull();

        Object restoredValue = wrapper.get();

        assertThat(restoredValue)
            .as("Cache entry value must not be null")
            .isNotNull()
            .isInstanceOf(CachedEventPage.class);

        CachedEventPage restored =
            (CachedEventPage) restoredValue;

        assertThat(restored.getContent())
            .as("Cached content must be deserialized")
            .isNotNull()
            .hasSize(1);

        assertThat(restored.getTotalElements())
            .isEqualTo(1);

        EventResponse restoredEvent =
            restored.getContent().getFirst();

        assertThat(restoredEvent)
            .isNotNull();

        assertThat(restoredEvent.getTitle())
            .isEqualTo("Cached Baku Event");

        Page<EventResponse> restoredPage =
            restored.toPage(
                PageRequest.of(0, 20)
            );

        assertThat(restoredPage.getContent())
            .hasSize(1);

        assertThat(restoredPage.getTotalElements())
            .isEqualTo(1);
    }
}
