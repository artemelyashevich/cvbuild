package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.AbstractRedisTest;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {RedisServiceImpl.class, ApplicationProperties.class})
@Import(RedisAutoConfiguration.class)
@EnableConfigurationProperties(ApplicationProperties.class)
@TestPropertySource(properties = {
        "app.cache.verification=300",
        "app.security.access-secret=dummy-secret-at-least-32-chars-long",
        "app.security.refresh-secret=dummy-secret-at-least-32-chars-long",
        "app.security.access-lifetime=3600",
        "app.security.refresh-lifetime=86400",
        "app.security.decode-signature=test"
})
class RedisServiceImplTest extends AbstractRedisTest {

    @Autowired
    private RedisServiceImpl redisService;

    @Test
    @DisplayName("putOtp: should save and retrieve otp from testcontainer")
    void putOtp_Success() {
        String key = "test-user";
        String otp = "123456";

        redisService.putOtp(key, otp);
        String result = redisService.getOtp(key);

        assertEquals(otp, result);
    }

    @Test
    @DisplayName("getLocation: should return default if not present")
    void getLocation_Default() {
        String result = redisService.getLocation("unknown@mail.com");
        assertEquals("en", result);
    }
}