package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.LimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void check(String userId, LimitType type, int capacity) {
        String banKey = "limit:ban:" + type.name() + ":" + userId;
        String countKey = "limit:count:" + type.name() + ":" + userId;

        if (redisTemplate.hasKey(banKey)) {
            throw new AppException("Limit error", 403);
        }

        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(countKey, 25, TimeUnit.HOURS);
        }

        if (currentCount != null && currentCount >= capacity) {
            redisTemplate.opsForValue().set(banKey, "banned", 24, TimeUnit.HOURS);
            redisTemplate.delete(countKey);
        }
    }
}
