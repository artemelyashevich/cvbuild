package com.bsu.cvbuilder.security;

import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Component
public class SecureDataCacheSingleton {

    private final Cache<@NonNull String, SecureData> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    private final Cache<@NonNull String, UserProfile> userProfileCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public SecureData getSecureData(String userId) {
        return cache.getIfPresent(userId);
    }

    public UserProfile get(String userId) {
        return userProfileCache.getIfPresent(userId);
    }

    public void set(String userId, SecureData data) {
        cache.put(userId, data);
    }

    public void set(UserProfile userProfile) {
        userProfileCache.put(userProfile.getId(), userProfile);
    }

    public void clearCache(String userId) {
        cache.invalidate(userId);
        userProfileCache.invalidate(userId);
    }
}