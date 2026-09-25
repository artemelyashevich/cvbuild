package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed lock on Redis. Without an explicit lease time Redisson's watchdog keeps the lock alive while
 * the owner runs and lets it expire if the instance dies, so a crashed instance never blocks a key forever.
 */
@Slf4j
@Service
public class LockServiceImpl implements LockService {

    private static final String KEY_PREFIX = "cvbuilder:lock:";
    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);

    private final RedissonClient redissonClient;
    private final Duration wait;

    @Autowired
    public LockServiceImpl(RedissonClient redissonClient) {
        this(redissonClient, DEFAULT_WAIT);
    }

    public LockServiceImpl(RedissonClient redissonClient, Duration wait) {
        this.redissonClient = redissonClient;
        this.wait = wait;
    }

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(KEY_PREFIX + key);
        acquire(lock, key);
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void acquire(RLock lock, String key) {
        try {
            if (!lock.tryLock(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn("Lock {} was not acquired within {}", key, wait);
                throw new AppException("Resource is busy, try again later", 409);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Interrupted while waiting for lock", e, 500);
        }
    }
}
