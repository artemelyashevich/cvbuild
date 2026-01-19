package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.LimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String LIMIT_SCRIPT =
            """
                    if redis.call('EXISTS', KEYS[1]) == 1 then return -1 end
                    local current = redis.call('INCR', KEYS[2])
                    if current == 1 then redis.call('EXPIRE', KEYS[2], ARGV[2]) end
                    if current > tonumber(ARGV[1]) then
                      redis.call('SET', KEYS[1], 'banned', 'EX', ARGV[3])
                      redis.call('DEL', KEYS[2])
                      return -2 end
                    return current
                    """;

    @Override
    public void check(String userId, LimitType type, int capacity) {
        String banKey = "limit:ban:" + type.name() + ":" + userId;
        String countKey = "limit:count:" + type.name() + ":" + userId;

        String windowSeconds = "90000"; // 25 h
        String banSeconds = "86400";    // 24 h

        RedisScript<Long> script = new DefaultRedisScript<>(LIMIT_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(banKey, countKey),
                String.valueOf(capacity), windowSeconds, banSeconds
        );

        if (result == -1) {
            log.warn("User {} is currently banned for {}", userId, type);
            throw new AppException("You are temporarily banned from using " + type, 403);
        }

        if (result == -2) {
            log.warn("User {} exceeded limit for {} and is now banned", userId, type);
            throw new AppException("Limit exceeded. You are banned for 24 hours", 403);
        }

        log.debug("User {} limit count for {}: {}", userId, type, result);
    }
}