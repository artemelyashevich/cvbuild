package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.user.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserStatsRepository;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private static final String CACHE_ID = "user_id_stat";

    private final UserStatsRepository userStatsRepository;

    @Override
    @CacheEvict(value = CACHE_ID)
    public UserStats save(UserStats userStats) {
        log.debug("Ensuring UserStats exists for user: {}", userStats.getUserId());

        return userStatsRepository.findById(userStats.getUserId())
                .orElseGet(() -> {
                    log.info("Creating new UserStats for user: {}", userStats.getUserId());
                    return userStatsRepository.save(userStats);
                });
    }

    @Override
    @Cacheable(value = CACHE_ID, key = "#id")
    public UserStats findByUserId(String id) {
        log.debug("Finding UserStats for user with id: {}", id);
        UserStats userStats = userStatsRepository.findByUserId(id).orElseThrow(
                () -> {
                    String message = "UserStats not found for user with id: " + id;
                    log.debug(message);
                    return new AppException(message, 404);
                }
        );
        log.debug("Found UserStats for user with id: {}", userStats.getUserId());
        return userStats;
    }

    @Transactional
    @CacheEvict(value = CACHE_ID, allEntries = true)
    public void incrementStats(String userId, Consumer<UserStats> updater) {
        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> UserStats.builder()
                        .userId(userId)
                        .currentMonthUsage(new UserStats.MonthlyUsage())
                        .build());

        checkAndResetMonthlyStats(stats);

        updater.accept(stats);
        userStatsRepository.save(stats);
    }

    private void checkAndResetMonthlyStats(UserStats stats) {
        LocalDateTime now = LocalDateTime.now();
        UserStats.MonthlyUsage monthly = stats.getCurrentMonthUsage();

        if (monthly == null) {
            stats.setCurrentMonthUsage(new UserStats.MonthlyUsage());
            return;
        }

        if (monthly.getPeriodStart().getMonth() != now.getMonth() ||
                monthly.getPeriodStart().getYear() != now.getYear()) {

            log.info("Resetting monthly stats for user {} for new month {}", stats.getUserId(), now.getMonth());

            stats.setCurrentMonthUsage(UserStats.MonthlyUsage.builder()
                    .periodStart(now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0))
                    .aiRequests(0)
                    .resumesCreated(0)
                    .jobAnalyses(0)
                    .build());
        }
    }
}
