package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.LockService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    private final RedissonClient redissonClient;

    @Override
    public RLock lock(String lockName) {
        return redissonClient.getLock("lock:" + lockName);
    }

    @Override
    public boolean tryLock(RLock lock, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <T> T withLock(String lockName, Supplier<T> supplier) {
        RLock lock = lock(lockName);
        boolean locked = false;
        try {
            locked = tryLock(lock, 500, 5000, TimeUnit.MILLISECONDS);
            if (!locked) {
                return null;
            }
            return supplier.get();
        } finally {
            if (locked) {
                unlock(lock);
            }
        }
    }
}