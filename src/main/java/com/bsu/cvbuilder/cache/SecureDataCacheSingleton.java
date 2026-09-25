package com.bsu.cvbuilder.cache;

import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.UUID;

/**
 * Per-instance cache. Every write or clear is announced on {@link #INVALIDATION_CHANNEL} so other instances drop
 * their copy for that user and reload it on next access. The TTL bounds staleness if a message is lost.
 */
@Slf4j
@Component
public class SecureDataCacheSingleton {

    public static final String INVALIDATION_CHANNEL = "cvbuilder:cache:secure-data:invalidate";
    private static final String SEPARATOR = "|";

    private final String instanceId = UUID.randomUUID().toString();
    private final StringRedisTemplate redisTemplate;

    private final Cache<@NonNull String, SecureData> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    private final Cache<@NonNull String, UserProfile> userProfileCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public SecureDataCacheSingleton(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SecureData getSecureData(String userId) {
        return cache.getIfPresent(userId);
    }

    public UserProfile get(String userId) {
        return userProfileCache.getIfPresent(userId);
    }

    /**
     * Fills the cache after a read from the database. Local only: the data did not change, others keep their copy.
     */
    public void putLocal(String userId, SecureData data) {
        cache.put(userId, data);
    }

    /**
     * @see #putLocal(String, SecureData)
     */
    public void putLocal(UserProfile userProfile) {
        userProfileCache.put(userProfile.getId(), userProfile);
    }

    /**
     * Stores data that was just written and tells other instances to drop their stale copy.
     */
    public void set(String userId, SecureData data) {
        cache.put(userId, data);
        publishInvalidation(userId);
    }

    public void set(UserProfile userProfile) {
        userProfileCache.put(userProfile.getId(), userProfile);
        publishInvalidation(userProfile.getId());
    }

    public void clearCache(String userId) {
        evictLocal(userId);
        publishInvalidation(userId);
    }

    /**
     * Handles a message from {@link #INVALIDATION_CHANNEL}; messages sent by this instance are ignored.
     */
    public void onInvalidation(String message) {
        int separator = message.indexOf(SEPARATOR);
        if (separator < 0) {
            log.warn("Malformed cache invalidation message: {}", message);
            return;
        }
        if (!instanceId.equals(message.substring(0, separator))) {
            evictLocal(message.substring(separator + 1));
        }
    }

    private void evictLocal(String userId) {
        cache.invalidate(userId);
        userProfileCache.invalidate(userId);
    }

    private void publishInvalidation(String userId) {
        try {
            redisTemplate.convertAndSend(INVALIDATION_CHANNEL, instanceId + SEPARATOR + userId);
        } catch (Exception e) {
            // Other instances fall back to the TTL; the local write already succeeded.
            log.error("Failed to publish cache invalidation for user {}", userId, e);
        }
    }
}
