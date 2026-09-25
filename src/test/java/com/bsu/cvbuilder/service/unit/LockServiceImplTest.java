package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.impl.LockServiceImpl;
import com.bsu.cvbuilder.support.RedisTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against a real Redis (see {@link RedisTestSupport}); two Redisson clients act as two application instances.
 */
class LockServiceImplTest {

    private static RedissonClient instanceA;
    private static RedissonClient instanceB;
    private static LockServiceImpl lockServiceA;
    private static LockServiceImpl lockServiceB;

    @BeforeAll
    static void setUp() {
        instanceA = RedisTestSupport.newRedisson();
        instanceB = RedisTestSupport.newRedisson();
        lockServiceA = new LockServiceImpl(instanceA, Duration.ofSeconds(10));
        lockServiceB = new LockServiceImpl(instanceB, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDown() {
        instanceA.shutdown();
        instanceB.shutdown();
    }

    @RepeatedTest(3)
    @DisplayName("withLock: at most one thread across two instances runs the action for a key")
    void withLock_ContendedKeyAcrossInstances_IsMutuallyExclusive() throws Exception {
        String key = uniqueKey();
        int threads = 8;
        int iterations = 25;
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                LockServiceImpl lockService = t % 2 == 0 ? lockServiceA : lockServiceB;
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        lockService.withLock(key, () -> {
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
                future.get(60, TimeUnit.SECONDS);
            }
        }

        assertEquals(1, maxInside.get());
    }

    @Test
    @DisplayName("withLock: is reentrant for the same thread and returns the action result")
    void withLock_Nested_IsReentrant() {
        String key = uniqueKey();

        String result = lockServiceA.withLock(key, () -> lockServiceA.withLock(key, () -> "done"));

        assertEquals("done", result);
    }

    @Test
    @DisplayName("withLock: fails with 409 instead of waiting forever while another instance holds the key")
    void withLock_HeldByOtherInstance_Throws409AfterWait() throws Exception {
        String key = uniqueKey();
        LockServiceImpl impatient = new LockServiceImpl(instanceB, Duration.ofMillis(300));
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> holder = CompletableFuture.runAsync(() -> lockServiceA.withLock(key, () -> {
            held.countDown();
            await(release);
            return null;
        }));
        assertTrue(held.await(5, TimeUnit.SECONDS));

        try {
            AppException ex = assertThrows(AppException.class, () -> impatient.withLock(key, () -> "never"));
            assertEquals(409, ex.getStatusCode());
        } finally {
            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("withLock: releases the lock when the action throws")
    void withLock_ActionThrows_ReleasesLock() {
        String key = uniqueKey();

        assertThrows(IllegalStateException.class, () -> lockServiceA.withLock(key, () -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals("next", new LockServiceImpl(instanceB, Duration.ofMillis(300)).withLock(key, () -> "next"));
    }

    @Test
    @DisplayName("withLock: a lock held by an instance that died expires and is taken over")
    void withLock_OwnerDies_LockExpires() {
        String key = uniqueKey();
        RedissonClient dying = RedisTestSupport.newRedisson(1_000);
        dying.getLock("cvbuilder:lock:" + key).lock();
        dying.shutdown();

        String result = new LockServiceImpl(instanceB, Duration.ofSeconds(5)).withLock(key, () -> "taken over");

        assertEquals("taken over", result);
    }

    private static String uniqueKey() {
        return "test:" + UUID.randomUUID();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
