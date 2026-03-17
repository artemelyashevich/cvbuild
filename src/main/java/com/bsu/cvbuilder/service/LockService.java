package com.bsu.cvbuilder.service;

import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface LockService {

    RLock lock(String lockName);

    boolean tryLock(RLock rLock, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(RLock lock);

    <T> T withLock(String lockName, Supplier<T> supplier);
}
