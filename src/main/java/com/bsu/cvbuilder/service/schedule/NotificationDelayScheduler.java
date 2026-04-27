package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDelayScheduler extends AbstractScheduler {

    private static final String MOVE_SCRIPT = """
            local zset = KEYS[1]
            local list = KEYS[2]
            local now = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])

            local items = redis.call('ZRANGEBYSCORE', zset, 0, now, 'LIMIT', 0, limit)

            for i, item in ipairs(items) do
                redis.call('ZREM', zset, item)
                redis.call('LPUSH', list, item)
            end

            return items
            """;

    private static final String JOB = "notification-delay";

    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 2000)
    public void processDelayedQueue() {

        execute(JOB, () -> {

            setEnabled(false);

            long now = System.currentTimeMillis();
            int batchSize = 100;

            List<String> moved = redisTemplate.execute(
                    (RedisCallback<List<String>>) connection ->
                            connection.eval(
                                    MOVE_SCRIPT.getBytes(),
                                    ReturnType.MULTI,
                                    2,
                                    CacheUtil.NOTIFICATION_DELAYED_KEY.getBytes(),
                                    CacheUtil.NOTIFICATION_RETRY_KEY.getBytes(),
                                    String.valueOf(now).getBytes(),
                                    String.valueOf(batchSize).getBytes()
                            )
            );

            int movedSize = (moved == null) ? 0 : moved.size();
            long duration = System.currentTimeMillis() - now;

            if (movedSize > 0) {
                setEnabled(true);
                log.info("{} {MOVE} moved={} from=delayed to=retry durationMs={}",
                        LOG_PREFIX, movedSize, duration);
            }
        });
    }
}