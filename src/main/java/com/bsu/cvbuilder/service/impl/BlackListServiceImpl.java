package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.BlackListService;
import com.bsu.cvbuilder.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class BlackListServiceImpl implements BlackListService {

    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void banToken(String token) {
        Date expiration = jwtService.extractExpiration(token);
        long duration = expiration.getTime() - System.currentTimeMillis();

        if (duration > 0) {
            redisTemplate.opsForValue().set(token, "revoked", Duration.ofMillis(duration));
        }

    }

    @Override
    public Boolean validate(String token) {
        return redisTemplate.opsForValue().get(token) != null;
    }
}
