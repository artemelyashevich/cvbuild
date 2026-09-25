package com.bsu.cvbuilder.support;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real Redis for tests of cross-instance behavior. Uses {@code TEST_REDIS_ADDRESS=host:port} when set
 * (e.g. a local redis-server), otherwise starts a Testcontainers Redis once per JVM.
 * Tests isolate themselves with unique keys instead of flushing.
 */
public final class RedisTestSupport {

    private static String host;
    private static int port;

    private RedisTestSupport() {
    }

    public static synchronized String host() {
        init();
        return host;
    }

    public static synchronized int port() {
        init();
        return port;
    }

    /**
     * A separate client stands in for a separate application instance.
     */
    public static RedissonClient newRedisson(long lockWatchdogTimeoutMillis) {
        Config config = new Config();
        config.setLockWatchdogTimeout(lockWatchdogTimeoutMillis);
        config.useSingleServer().setAddress("redis://%s:%d".formatted(host(), port()));
        return Redisson.create(config);
    }

    public static RedissonClient newRedisson() {
        return newRedisson(30_000);
    }

    public static LettuceConnectionFactory newConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host(), port()));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    private static void init() {
        if (host != null) {
            return;
        }
        String address = System.getenv("TEST_REDIS_ADDRESS");
        if (address != null && !address.isBlank()) {
            String[] parts = address.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
            return;
        }
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        container.start();
        host = container.getHost();
        port = container.getMappedPort(6379);
    }
}
