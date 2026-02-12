package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.domain.event.*;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserStatsService userStatsService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleUserLoginEvent(LoginEvent userLoginEvent) {
        String userId = userLoginEvent.getUserId();
        UserProfile userProfile = userLoginEvent.getUserProfile();

        log.debug("Received UserLoginEvent for userId {}", userId);

        userProfile.setLastLogin(LocalDateTime.now());
        userProfileService.update(userProfile);

        userStatsService.incrementStats(userId, stats -> {
            stats.setTotalViews(stats.getTotalViews() + 1);
        });

        if (userProfile.getEmail() == null || userProfile.getEmail().isEmpty()) {
            notificationService.sendNotification(NotificationDto.builder()
                    .parameters(Map.of("message", "Please, provide and verify your email"))
                    .engine(NotificationEngine.WS)
                    .receiver(userProfile.getLogin())
                    .templateName("")
                    .build());
        }
    }

    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        String userId = event.getUser().getId();
        log.debug("Initializing stats for user: {}", userId);
        userStatsService.save(UserStats.builder().userId(userId).build());
    }

    @Async
    @EventListener
    public void handleDownload(DownloadResumeEvent event) {
        updateStat(event.getUserId(), "totalDownloads",
                stats -> stats.setTotalDownloads(stats.getTotalDownloads() + 1));
    }

    @Async
    @EventListener
    public void handleAiRequest(UserGenerateNewMessageEvent event) {
        updateStat(event.getUserId(), "aiRequestsUsed", stats -> {
            stats.setAiRequestsUsed(stats.getAiRequestsUsed() + 1);
            var monthly = stats.getCurrentMonthUsage();
            monthly.setAiRequests(monthly.getAiRequests() + 1);
        });
    }

    @Async
    @EventListener
    public void handleResumeCreated(CreateResumeEvent event) {
        updateStat(event.getUserId(), "resumesCreated", stats -> {
            stats.setResumesCreated(stats.getResumesCreated() + 1);
            var monthly = stats.getCurrentMonthUsage();
            monthly.setResumesCreated(monthly.getResumesCreated() + 1);
        });
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
