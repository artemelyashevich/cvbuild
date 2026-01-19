package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisServiceImplTest extends AbstractTest {

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