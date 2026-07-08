package com.loopin.api.service.implementation;

import com.loopin.api.entity.User;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final long TTL_SECONDS = 360L;

    @Override
    public void handleUserConnect(Long userId) {
        if (userId == null) return;
        try {
            redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, "true", TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to set presence in Redis for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleUserDisconnect(Long userId) {
        if (userId == null) return;
        try {
            redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to delete presence in Redis for user {}: {}", userId, e.getMessage());
        }

        try {
            userRepository.findById(userId).ifPresent(user -> {
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
            });
        } catch (Exception e) {
            log.warn("Failed to update lastSeen in DB for user {}: {}", userId, e.getMessage());
        }
    }
}
