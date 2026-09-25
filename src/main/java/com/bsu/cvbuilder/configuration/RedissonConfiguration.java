package com.bsu.cvbuilder.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson is used only for cross-instance coordination (locks, rate limits).
 * Caching and pub/sub keep using the Lettuce connection configured by Spring Boot.
 */
@Configuration
public class RedissonConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisConnectionDetails connectionDetails) {
        RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://%s:%d".formatted(standalone.getHost(), standalone.getPort()))
                .setDatabase(standalone.getDatabase());
        if (connectionDetails.getUsername() != null) {
            server.setUsername(connectionDetails.getUsername());
        }
        if (connectionDetails.getPassword() != null) {
            server.setPassword(connectionDetails.getPassword());
        }
        return Redisson.create(config);
    }
}
