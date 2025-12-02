package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.MessageSourceService;
import com.bsu.cvbuilder.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSourceServiceImpl implements MessageSourceService {

    private final MessageSource messageSource;
    private final RedisService redisService;

    @Override
    public String findMessage(String key, String email) {
        log.debug("Attempting to find message for key {} from boundle resources", key);
        return messageSource.getMessage(key, null, Locale.of(redisService.getLocation(email)));
    }
}
