package com.loopin.api.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnProperty(
    name = "spring.cache.type",
    havingValue = "redis",
    matchIfMissing = true
)
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
        RedisConnectionFactory connectionFactory
    ) {
        GenericJacksonJsonRedisSerializer serializer =
            GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();

        RedisCacheConfiguration config =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                    RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(
                            new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                    RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(serializer)
                );

        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
