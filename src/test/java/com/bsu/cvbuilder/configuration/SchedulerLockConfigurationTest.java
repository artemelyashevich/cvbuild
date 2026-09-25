package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.support.RedisTestSupport;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {SchedulerLockConfiguration.class, SchedulerLockConfigurationTest.TestConfig.class})
class SchedulerLockConfigurationTest {

    @Autowired
    private LockedJob lockedJob;

    @Test
    @DisplayName("@SchedulerLock: a job that is already running elsewhere is skipped, not run twice")
    void schedulerLock_ConcurrentRuns_OnlyOneExecutes() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Void>> runs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            runs.add(CompletableFuture.runAsync(() -> {
                await(start);
                lockedJob.run();
            }));
        }
        start.countDown();
        CompletableFuture.allOf(runs.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);

        assertEquals(1, lockedJob.executions());
    }

    @Test
    @DisplayName("every @Scheduled method in the application is guarded by @SchedulerLock")
    void everyScheduledMethod_HasSchedulerLock() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        List<String> unguarded = new ArrayList<>();
        int scheduled = 0;
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.bsu.cvbuilder.service")) {
            Class<?> type = ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader());
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Scheduled.class)) {
                    scheduled++;
                    if (!method.isAnnotationPresent(SchedulerLock.class)) {
                        unguarded.add(type.getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }

        assertTrue(scheduled > 0, "no @Scheduled methods found, scan is broken");
        assertEquals(List.of(), unguarded);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class LockedJob {
        private final AtomicInteger executions = new AtomicInteger();

        // Read through a method: the bean is a ShedLock proxy, its own fields are not the target's.
        public int executions() {
            return executions.get();
        }

        @SchedulerLock(name = "scheduler-lock-configuration-test", lockAtMostFor = "PT10S")
        public void run() {
            executions.incrementAndGet();
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Configuration
    static class TestConfig {

        @Bean(destroyMethod = "destroy")
        RedisConnectionFactory redisConnectionFactory() {
            return RedisTestSupport.newConnectionFactory();
        }

        @Bean
        LockedJob lockedJob() {
            return new LockedJob();
        }
    }
}
