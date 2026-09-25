package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.cache.SecureDataCacheSingleton;
import com.bsu.cvbuilder.configuration.RedisPubSubConfiguration;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.impl.WsNotificationStrategyImpl;
import com.bsu.cvbuilder.support.RedisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Two cache instances with their own listener containers on one real Redis act as two application instances.
 */
class SecureDataCacheInvalidationTest {

    private LettuceConnectionFactory connectionFactory;
    private RedisMessageListenerContainer containerA;
    private RedisMessageListenerContainer containerB;
    private SecureDataCacheSingleton cacheA;
    private SecureDataCacheSingleton cacheB;

    @BeforeEach
    void setUp() throws Exception {
        connectionFactory = RedisTestSupport.newConnectionFactory();
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        cacheA = new SecureDataCacheSingleton(template);
        cacheB = new SecureDataCacheSingleton(template);
        RedisPubSubConfiguration configuration = new RedisPubSubConfiguration();
        containerA = start(configuration.redisMessageListenerContainer(connectionFactory, cacheA, mock(WsNotificationStrategyImpl.class)));
        containerB = start(configuration.redisMessageListenerContainer(connectionFactory, cacheB, mock(WsNotificationStrategyImpl.class)));
    }

    @AfterEach
    void tearDown() throws Exception {
        containerA.destroy();
        containerB.destroy();
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("set on one instance evicts the stale copy on the other and keeps the writer's copy")
    void set_EvictsOtherInstanceOnly() {
        String userId = UUID.randomUUID().toString();
        UserProfile stale = UserProfile.builder().id(userId).email("old@mail.test").build();
        UserProfile fresh = UserProfile.builder().id(userId).email("new@mail.test").build();
        cacheB.putLocal(stale);

        cacheA.set(fresh);

        assertTrue(eventually(() -> cacheB.get(userId) == null), "other instance still has the stale profile");
        assertSame(fresh, cacheA.get(userId));
    }

    @Test
    @DisplayName("clearCache on one instance evicts secure data and profile on the other")
    void clearCache_EvictsEverywhere() {
        String userId = UUID.randomUUID().toString();
        cacheB.putLocal(userId, new SecureData());
        cacheB.putLocal(UserProfile.builder().id(userId).build());

        cacheA.clearCache(userId);

        assertTrue(eventually(() -> cacheB.getSecureData(userId) == null && cacheB.get(userId) == null));
    }

    @Test
    @DisplayName("putLocal after a database read does not evict other instances")
    void putLocal_DoesNotEvictOthers() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UserProfile profile = UserProfile.builder().id(userId).build();
        cacheB.putLocal(profile);

        cacheA.putLocal(UserProfile.builder().id(userId).build());
        Thread.sleep(300);

        assertSame(profile, cacheB.get(userId));
    }

    private static RedisMessageListenerContainer start(RedisMessageListenerContainer container) throws Exception {
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    private static boolean eventually(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.onSpinWait();
        }
        return condition.getAsBoolean();
    }
}
