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
public class HistoryCleanupScheduler extends AbstractScheduler {

    private static final Duration DUPLICATE_TTL = Duration.ofMinutes(10);
    private static final String JOB = "history-cleanup";

    private final HistoryRepository historyRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000, scheduler = "cleanUpExecutor")
    @Monitored(value = "scheduling.history", context = "cleanup")
    public void cleanupHistoryDuplicates() {

        execute(JOB, () -> {

            List<History> histories = historyRepository.findWithMultipleEvents();

            int totalDocs = histories.size();
            int updatedDocs = 0;
            int totalRemoved = 0;

            LocalDateTime now = LocalDateTime.now();

            log.info("{} {cleanup} started docs={}", LOG_PREFIX, totalDocs);

            for (History history : histories) {

                try {
                    int removed = processHistory(history, now);

                    if (removed > 0) {
                        historyRepository.save(history);

                        updatedDocs++;
                        totalRemoved += removed;

                        log.info("{} {cleanup} userId={} removed={}",
                                LOG_PREFIX,
                                history.getUserId(),
                                removed);
                    }

                } catch (Exception ex) {
                    log.error("{} {cleanup} failed userId={}",
                            LOG_PREFIX,
                            history.getUserId(),
                            ex);
                }
            }

            log.info("{} {cleanup} finished docs={} updated={} removed={}",
                    LOG_PREFIX,
                    totalDocs,
                    updatedDocs,
                    totalRemoved);
        });
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
        List<String> groupKeys = new ArrayList<>();

        for (Map.Entry<String, String> entry : sorted) {

            String key = entry.getKey();
            String eventType = entry.getValue();

            if (eventType == null) {
                events.remove(key);
                removedCount++;

                log.debug("{} {cleanup} null-event removed key={} userId={}",
                        LOG_PREFIX, key, history.getUserId());
                continue;
            }

            if (!Objects.equals(previousEvent, eventType)) {
                removedCount += cleanupGroup(groupKeys, previousEvent, events, now, history.getUserId());
                groupKeys.clear();
                previousEvent = eventType;
            }

            groupKeys.add(key);
        }

        removedCount += cleanupGroup(groupKeys, previousEvent, events, now, history.getUserId());

        return removedCount;
    }

    private int cleanupGroup(List<String> groupKeys,
                             String eventType,
                             Map<String, String> events,
                             LocalDateTime now,
                             String userId) {

        if (groupKeys.size() <= 1 || eventType == null) {
            return 0;
        }

        String lastKey = groupKeys.get(groupKeys.size() - 1);

        LocalDateTime lastTime;
        try {
            lastTime = extractTime(lastKey);
        } catch (DateTimeParseException e) {
            log.warn("{} {cleanup} invalid timestamp key={} userId={}",
                    LOG_PREFIX, lastKey, userId);
            return 0;
        }

        if (!lastTime.plus(DUPLICATE_TTL).isBefore(now)) {
            return 0;
        }

        int removed = groupKeys.size() - 1;

        for (int i = 0; i < groupKeys.size() - 1; i++) {
            events.remove(groupKeys.get(i));
        }

        log.debug("{} {cleanup} group processed userId={} eventType={} removed={}",
                LOG_PREFIX,
                userId,
                eventType,
                removed);

        return removed;
    }

    private LocalDateTime extractTime(String key) {
        int idx = key.indexOf('_');
        String timePart = (idx > 0) ? key.substring(0, idx) : key;
        return LocalDateTime.parse(timePart);
    }
}