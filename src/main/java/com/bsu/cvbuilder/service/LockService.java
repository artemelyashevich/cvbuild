package com.bsu.cvbuilder.service;


import java.util.function.Supplier;

public interface LockService {

    /**
     * Runs {@code supplier} while holding a lock that is exclusive across all application instances.
     * Reentrant for the same thread.
     *
     * @throws com.bsu.cvbuilder.exception.AppException 409 when the lock is not acquired in time
     */
    <T> T withLock(String lockName, Supplier<T> supplier);
}
