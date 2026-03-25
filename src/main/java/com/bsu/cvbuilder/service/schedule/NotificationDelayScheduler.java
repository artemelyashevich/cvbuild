package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.util.LockUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.bsu.cvbuilder.util.CacheUtil.NOTIFICATION_DELAYED_KEY;

@Service
@RequiredArgsConstructor
public class NotificationDelayScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final LockService lockService;

    @Scheduled(fixedRate = 2000)
    public void processDelayedQueue() {
        lockService.withLock(LockUtil.NOTIFICATION_DQL, () -> {
            long now = System.currentTimeMillis();

            Set<String> ready = redisTemplate.opsForZSet()
                    .rangeByScore(NOTIFICATION_DELAYED_KEY, 0, now);

            if (ready == null || ready.isEmpty()) return null;

            for (String item : ready) {

                redisTemplate.opsForZSet()
                        .remove(NOTIFICATION_DELAYED_KEY, item);

                redisTemplate.opsForList()
                        .leftPush("notification:retry", item);
            }

            return null;
        });
    }
}
