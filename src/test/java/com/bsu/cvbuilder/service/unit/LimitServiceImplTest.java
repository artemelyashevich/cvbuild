package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.impl.LimitServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private LimitServiceImpl limitService;

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.service.provider.LimitTestData#provideLimitCheckScenarios")
    @DisplayName("Check Limit - Various Scenarios - Validates behavior based on Redis response")
    void check_VaryingRedisResponses_BehavesCorrectly(
            String userId, 
            LimitType type, 
            int capacity, 
            Long redisResponse, 
            boolean expectException) {
        
        // Arrange
        var banKey = "limit:ban:" + type.name() + ":" + userId;
        var countKey = "limit:count:" + type.name() + ":" + userId;

        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(banKey, countKey)),
                eq(String.valueOf(capacity)), eq("90000"), eq("86400")
        )).thenReturn(redisResponse);

        // Act & Assert
        if (expectException) {
            var exception = assertThrows(AppException.class, 
                () -> limitService.check(userId, type, capacity));
            
            assertEquals(403, exception.getStatusCode());
            assertTrue(exception.getMessage().contains("ban") || exception.getMessage().contains("Limit exceeded"));
        } else {
            assertDoesNotThrow(() -> limitService.check(userId, type, capacity));
        }
    }
}