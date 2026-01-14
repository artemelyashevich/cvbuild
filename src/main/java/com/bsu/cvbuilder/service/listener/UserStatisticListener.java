package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.entity.user.UserStats;
import com.bsu.cvbuilder.domain.event.user.UserCreatedEvent;
import com.bsu.cvbuilder.domain.event.user.UserCreatedResumeEvent;
import com.bsu.cvbuilder.domain.event.user.UserDownloadedResumeEvent;
import com.bsu.cvbuilder.domain.event.user.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatisticListener {

    private final UserStatsService userStatsService;

    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        String userId = event.getUser().getId();
        log.debug("Initializing stats for user: {}", userId);
        userStatsService.save(UserStats.builder().userId(userId).build());
    }

    @Async
    @EventListener
    public void handleAiRequest(UserGenerateNewMessageEvent event) {
        updateStat(event.getUserId(), "aiRequestsUsed",
                stats -> stats.setAiRequestsUsed(stats.getAiRequestsUsed() + 1));
    }

    @Async
    @EventListener
    public void handleDownload(UserDownloadedResumeEvent event) {
        updateStat(event.getUserId(), "totalDownloads",
                stats -> stats.setTotalDownloads(stats.getTotalDownloads() + 1));
    }

    @Async
    @EventListener
    public void handleResumeCreated(UserCreatedResumeEvent event) {
        updateStat(event.getUserId(), "resumesCreated",
                stats -> stats.setResumesCreated(stats.getResumesCreated() + 1));
    }

    private void updateStat(String userId, String statName, Consumer<UserStats> action) {
        log.debug("Updating {} for user: {}", statName, userId);
        try {
            userStatsService.incrementStats(userId, action);
            log.debug("Successfully updated {} for user: {}", statName, userId);
        } catch (Exception e) {
            log.error("Failed to update {} for user: {}", statName, userId, e);
        }
    }
}
