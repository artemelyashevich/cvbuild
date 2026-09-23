package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.service.impl.LockServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LockServiceImplTest {

    private final LockServiceImpl lockService = new LockServiceImpl();

    @RepeatedTest(5)
    @DisplayName("withLock: at most one thread runs the action for a key, even as locks are released and recreated")
    void withLock_ContendedKey_IsMutuallyExclusive() throws Exception {
        int threads = 16;
        int iterations = 500;
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        lockService.withLock("key", () -> {
                            maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                            Thread.onSpinWait();
                            inside.decrementAndGet();
                            return null;
                        });
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        assertEquals(1, maxInside.get());
    }

    @Test
    @DisplayName("withLock: is reentrant for the same thread and returns the action result")
    void withLock_Nested_IsReentrant() {
        String result = lockService.withLock("key", () -> lockService.withLock("key", () -> "done"));

        assertEquals("done", result);
    }
}
