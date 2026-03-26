package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());

        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();

            if (!lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }

    @Override
    public void clear() {
        locks.clear();
    }
}