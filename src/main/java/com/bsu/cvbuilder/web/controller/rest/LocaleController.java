package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.RedisService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locale")
@RequiredArgsConstructor
public class LocaleController {

    private final RedisService redisService;
    private final SecurityService securityService;

    @PostMapping("{locale}")
    public void setLocale(@PathVariable String locale) {
        redisService.putLocation(securityService.findCurrentUser().getEmail(), locale);
    }
}
