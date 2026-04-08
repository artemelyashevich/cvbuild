package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.entity.History;
import com.bsu.cvbuilder.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class        HistoryCleanupScheduler {

    private static final Duration DUPLICATE_TTL = Duration.ofMinutes(10);

    private final HistoryRepository historyRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000, scheduler = "cleanUpExecutor")
    @Monitored(value = "scheduling.history", context = "cleanup")
    public void cleanupHistoryDuplicates() {

        long start = System.currentTimeMillis();
        log.info("[CLEAN UP JOB]: History duplicate cleanup started");

        List<History> histories = historyRepository.findWithMultipleEvents();

        int totalDocs = histories.size();
        int updatedDocs = 0;
        int totalRemoved = 0;

        LocalDateTime now = LocalDateTime.now();

        for (History history : histories) {

            try {
                int removed = processHistory(history, now);
                if (removed > 0) {
                    historyRepository.save(history);
                    updatedDocs++;
                    totalRemoved += removed;

                    log.debug("User {}: removed {} duplicate events",
                            history.getUserId(), removed);
                }
            } catch (Exception e) {
                log.error("Failed to cleanup history for user {}",
                        history.getUserId(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;

        log.info("""
                [CLEAN UP JOB]:
                History duplicate cleanup finished:
                - Documents scanned: {}
                - Documents updated: {}
                - Duplicates removed: {}
                - Execution time: {} ms
                """,
                totalDocs,
                updatedDocs,
                totalRemoved,
                duration
        );
    }

    private int processHistory(History history, LocalDateTime now) {

        Map<String, String> events = history.getEvents();
        if (events == null || events.size() <= 1) {
            return 0;
        }

        List<Map.Entry<String, String>> sorted = events.entrySet()
                .stream()
                .filter(e -> e.getKey() != null)
                .sorted(Map.Entry.comparingByKey())
                .toList();

        int removedCount = 0;

        String previousEvent = null;
        List<String> currentGroupKeys = new ArrayList<>();

        for (Map.Entry<String, String> entry : sorted) {

            String key = entry.getKey();
            String eventType = entry.getValue();

            if (eventType == null) {
                events.remove(key);
                removedCount++;
                continue;
            }

            if (!Objects.equals(previousEvent, eventType)) {

                removedCount += cleanupGroup(currentGroupKeys, previousEvent, events, now);

                currentGroupKeys.clear();
                previousEvent = eventType;
            }

            currentGroupKeys.add(key);
        }

        removedCount += cleanupGroup(currentGroupKeys, previousEvent, events, now);

        return removedCount;
    }

    private int cleanupGroup(List<String> groupKeys,
                             String eventType,
                             Map<String, String> events,
                             LocalDateTime now) {

        if (groupKeys.size() <= 1 || eventType == null) {
            return 0;
        }

        String lastKey = groupKeys.getLast();

        LocalDateTime lastTime;
        try {
            lastTime = extractTime(lastKey);
        } catch (DateTimeParseException e) {
            log.warn("Invalid history timestamp key: {}", lastKey);
            return 0;
        }

        if (!lastTime.plus(DUPLICATE_TTL).isBefore(now)) {
            return 0;
        }

        int removed = 0;

        for (int i = 0; i < groupKeys.size() - 1; i++) {
            events.remove(groupKeys.get(i));
            removed++;
        }

        return removed;
    }

    private LocalDateTime extractTime(String key) {
        int separatorIndex = key.indexOf('_');
        String timePart = separatorIndex > 0 ? key.substring(0, separatorIndex) : key;
        return LocalDateTime.parse(timePart);
    }
}
