package com.bsu.cvbuilder.cache;

import com.bsu.cvbuilder.service.ws.VoiceSession;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AudioWSCache {

    private final Cache<@NonNull String, VoiceSession> cache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(Duration.ofMinutes(20))
                    .removalListener((String sessionId, VoiceSession voiceSession, RemovalCause cause) -> {
                        if (voiceSession != null) {
                            voiceSession.close();
                        }
                    })
                    .build();

    public VoiceSession get(@NonNull String sessionId) {
        return cache.getIfPresent(sessionId);
    }

    public void put(
            @NonNull String sessionId,
            @NonNull VoiceSession voiceSession
    ) {
        cache.put(sessionId, voiceSession);
    }

    public void remove(@NonNull String sessionId) {
        cache.invalidate(sessionId);
    }
}
