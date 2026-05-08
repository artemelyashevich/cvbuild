package com.bsu.cvbuilder.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.vosk.Recognizer;

import java.time.Duration;

@Component
public class AudioWSCache {

    private final Cache<@NonNull String, Recognizer> cache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(Duration.ofMinutes(20))
                    .build();

    public Recognizer get(@NonNull String sessionId) {
        return cache.getIfPresent(sessionId);
    }

    public void put(
            @NonNull String sessionId,
            @NonNull Recognizer recognizer
    ) {
        cache.put(sessionId, recognizer);
    }

    public void remove(@NonNull String sessionId) {
        Recognizer recognizer = cache.getIfPresent(sessionId);

        if (recognizer != null) {
            recognizer.close();
        }

        cache.invalidate(sessionId);
    }
}