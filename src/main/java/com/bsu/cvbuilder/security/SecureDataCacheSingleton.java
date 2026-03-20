package com.bsu.cvbuilder.security;

import com.bsu.cvbuilder.domain.entity.SecureData;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecureDataCacheSingleton {
    private final Map<String, SecureData> cache = new ConcurrentHashMap<>();

    public SecureData get(String userId) {
        return cache.get(userId);
    }

    public void set(String userId, SecureData data) {
        cache.put(userId, data);
    }

    public void clear(String userId) {
        cache.remove(userId);
    }
}