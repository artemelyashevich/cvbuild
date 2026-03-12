package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureDataCleanupScheduler {

    private final SecureDataRepository repository;

    @Scheduled(fixedRate = 5 * 60 * 1000, scheduler = "cleanUpExecutor")
    @Monitored(value = "scheduling.secure_data", context = "cleanup")
    public void cleanupExpiredSecureEvents() {

        long start = System.currentTimeMillis();

        log.info("[CLEAN UP JOB]: SecureData cleanup job started");

        List<SecureData> all = repository.findWithMultipleSecureEvents();

        int totalDocuments = all.size();
        int documentsWithEvents = 0;
        int updatedDocuments = 0;
        int totalEventsChecked = 0;
        int totalEventsRemoved = 0;

        for (SecureData secureData : all) {

            boolean changed = false;

            Map<SecureEvent, List<LocalDateTime>> events = secureData.getSecureEvents();
            if (events == null || events.isEmpty()) {
                continue;
            }

            documentsWithEvents++;

            for (Iterator<Map.Entry<SecureEvent, List<LocalDateTime>>> it = events.entrySet().iterator(); it.hasNext();) {
                Map.Entry<SecureEvent, List<LocalDateTime>> entry = it.next();

                SecureEvent eventType = entry.getKey();
                List<LocalDateTime> timestamps = entry.getValue();

                if (timestamps == null || timestamps.size() <= 1) {
                    continue;
                }

                totalEventsChecked += timestamps.size();

                LocalDateTime now = LocalDateTime.now();

                int before = timestamps.size();

                timestamps.removeIf(time ->
                        timestamps.size() > 1 &&
                                time.plus(eventType.getDuration()).isBefore(now)
                );

                int removed = before - timestamps.size();
                if (removed > 0) {
                    totalEventsRemoved += removed;
                    changed = true;

                    log.debug("UserId={} | Event={} | Removed {} expired entries | Remaining={}",
                            secureData.getUserId(),
                            eventType.name(),
                            removed,
                            timestamps.size());
                }
            }

            if (changed) {
                repository.save(secureData);
                updatedDocuments++;
            }
        }

        long duration = System.currentTimeMillis() - start;

        log.info("""
                [CLEAN UP JOB]:
                SecureData cleanup job finished:
                - Total documents scanned: {}
                - Documents with events: {}
                - Documents updated: {}
                - Total event entries checked: {}
                - Total expired entries removed: {}
                - Execution time: {} ms
                """,
                totalDocuments,
                documentsWithEvents,
                updatedDocuments,
                totalEventsChecked,
                totalEventsRemoved,
                duration
        );
    }
}
