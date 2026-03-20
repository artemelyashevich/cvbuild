package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.BlackListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class BlackListServiceImpl implements BlackListService {

    private static final String BLACK_LIST_PREFIX = "ban::login";

    private final RedisTemplate<String, String> redisTemplate;


    @Override
    public void banToken(String token, Date expiration) {
        long duration = expiration.getTime() - System.currentTimeMillis();

        if (duration > 0) {
            redisTemplate.opsForValue().set(token, "revoked", Duration.ofMillis(duration));
        }

    }

    @Override
    public Boolean validate(String token) {
        return redisTemplate.opsForValue().get(token) != null;
    }

    @Override
    public void banUser(String login) {
        redisTemplate.opsForValue().set(BLACK_LIST_PREFIX, login, Duration.ofMillis(System.currentTimeMillis()));
    }
}
