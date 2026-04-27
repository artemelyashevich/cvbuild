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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureDataCleanupScheduler extends AbstractScheduler {

    private static final String JOB = "secure-data-cleanup";
    private static final String LOG_PREFIX = "{[SECURE_DATA_CLEANUP]}";

    private final SecureDataRepository repository;

    @Scheduled(fixedRate = 5 * 60 * 1000, scheduler = "cleanUpExecutor")
    @Monitored(value = "scheduling.secure_data", context = "cleanup")
    public void cleanupExpiredSecureEvents() {

        execute(JOB, () -> {

            long start = System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();

            List<SecureData> all = repository.findWithMultipleSecureEvents();

            int totalDocuments = all.size();
            int documentsWithEvents = 0;
            int updatedDocuments = 0;
            int totalEventsChecked = 0;
            int totalEventsRemoved = 0;

            log.debug("{} {cleanup} started documents={}", LOG_PREFIX, totalDocuments);

            for (SecureData secureData : all) {

                Map<SecureEvent, List<LocalDateTime>> events = secureData.getSecureEvents();

                if (events == null || events.isEmpty()) {
                    continue;
                }

                documentsWithEvents++;

                boolean changed = false;

                for (var entry : events.entrySet()) {

                    SecureEvent eventType = entry.getKey();
                    List<LocalDateTime> timestamps = entry.getValue();

                    if (timestamps == null || timestamps.size() <= 1) {
                        continue;
                    }

                    totalEventsChecked += timestamps.size();

                    int before = timestamps.size();

                    timestamps.removeIf(time ->
                            timestamps.size() > 1 &&
                                    time.plus(eventType.getDuration()).isBefore(now)
                    );

                    int removed = before - timestamps.size();

                    if (removed > 0) {
                        totalEventsRemoved += removed;
                        changed = true;

                        log.debug("{} {cleanup} expired-events removed userId={} event={} removed={} remaining={}",
                                LOG_PREFIX,
                                secureData.getUserId(),
                                eventType,
                                removed,
                                timestamps.size());
                    }
                }

                if (changed) {
                    repository.save(secureData);
                    updatedDocuments++;

                    log.info("{} {cleanup} document-updated userId={}",
                            LOG_PREFIX,
                            secureData.getUserId());
                }
            }

            long duration = System.currentTimeMillis() - start;

            log.info("{} {cleanup} finished scannedDocs={} docsWithEvents={} updatedDocs={} checkedEvents={} removedEvents={} durationMs={}",
                    LOG_PREFIX,
                    totalDocuments,
                    documentsWithEvents,
                    updatedDocuments,
                    totalEventsChecked,
                    totalEventsRemoved,
                    duration);
        });
    }
}
