package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.cache.SecureDataCacheSingleton;
import com.bsu.cvbuilder.service.impl.WsNotificationStrategyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Cross-instance signals over Redis pub/sub: cache invalidation and WebSocket notification fan-out.
 */
@Configuration
public class RedisPubSubConfiguration {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       SecureDataCacheSingleton secureDataCache,
                                                                       WsNotificationStrategyImpl wsNotificationStrategy) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        subscribe(container, SecureDataCacheSingleton.INVALIDATION_CHANNEL, secureDataCache::onInvalidation);
        subscribe(container, WsNotificationStrategyImpl.BROADCAST_CHANNEL, wsNotificationStrategy::onBroadcast);
        return container;
    }

    private static void subscribe(RedisMessageListenerContainer container, String channel, Consumer<String> handler) {
        container.addMessageListener(
                (message, pattern) -> handler.accept(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(channel));
    }
}
