package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private static final String LOCALE_KEY = "locale::%s";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void putOtp(String keyUnique, String otp) {
        redisTemplate.opsForValue().set("otp%s".formatted(keyUnique), otp, 120, TimeUnit.SECONDS);
    }

    @Override
    public String getOtp(String keyUnique) {
        var otp = redisTemplate.opsForValue().get("otp::%s".formatted(keyUnique));
        if (otp == null) {
            throw new AppException("Otp expired", 400);
        }
        return otp;
    }

    @Override
    public void putLocation(String email, String location) {
        redisTemplate.opsForValue().set(String.format(LOCALE_KEY, email), location, 10, TimeUnit.MINUTES);
    }

    @Override
    public String getLocation(String email) {
        var locale =  redisTemplate.opsForValue().get(String.format(LOCALE_KEY, email));
        if (locale == null) {
            locale = Locale.ENGLISH.getLanguage();
        }
        return locale;
    }
}