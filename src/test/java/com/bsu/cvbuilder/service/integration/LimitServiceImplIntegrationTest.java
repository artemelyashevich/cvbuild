package com.bsu.cvbuilder.service.integration;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.impl.LimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

class LimitServiceImplIntegrationTest extends AbstractTest {

    private LimitServiceImpl limitService;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        var connectionFactory = new LettuceConnectionFactory(
                redisContainer.getHost(), 
                redisContainer.getFirstMappedPort()
        );
        connectionFactory.afterPropertiesSet();
        
        this.redisTemplate = new StringRedisTemplate(connectionFactory);
        this.limitService = new LimitServiceImpl(redisTemplate);
        
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("check_SequenceRequest_IncrementsCountAndBansWhenExceeded")
    void check_SequenceRequest_SuccessThenBan() {
        var userId = "test-user";
        var type = LimitType.AI_MESSAGE;
        var capacity = 2;

        assertDoesNotThrow(() -> limitService.check(userId, type, capacity));

        assertDoesNotThrow(() -> limitService.check(userId, type, capacity));

        var ex = assertThrows(AppException.class, 
                () -> limitService.check(userId, type, capacity));
        assertTrue(ex.getMessage().contains("Limit exceeded"));

        var banEx = assertThrows(AppException.class, 
                () -> limitService.check(userId, type, capacity));
        assertTrue(banEx.getMessage().contains("temporarily banned"));
    }

    @Test
    @DisplayName("check_AfterBanExpires_AllowsRequestsAgain")
    void check_ExpiredBan_AllowsAccess() {
        var userId = "expire-user";
        var banKey = "limit:ban:AI_MESSAGE:" + userId;

        redisTemplate.opsForValue().set(banKey, "banned");
        
        assertThrows(AppException.class, () -> limitService.check(userId, LimitType.AI_MESSAGE, 5));

        redisTemplate.delete(banKey);
        
        assertDoesNotThrow(() -> limitService.check(userId, LimitType.AI_MESSAGE, 5));
    }
}