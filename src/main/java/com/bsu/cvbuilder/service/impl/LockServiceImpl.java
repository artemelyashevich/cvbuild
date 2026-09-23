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

    private final ConcurrentHashMap<String, LockHolder> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        LockHolder holder = locks.compute(key, (k, existing) -> {
            LockHolder h = existing == null ? new LockHolder() : existing;
            h.users++;
            return h;
        });

        holder.lock.lock();
        try {
            return action.get();
        } finally {
            holder.lock.unlock();
            // existing is null only if clear() ran while the lock was held (shutdown)
            locks.compute(key, (k, existing) -> existing == null || --existing.users == 0 ? null : existing);
        }
    }

    @Override
    public void clear() {
        locks.clear();
    }

    private static final class LockHolder {

        private final ReentrantLock lock = new ReentrantLock();
        private int users;
    }
}
