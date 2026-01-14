package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.user.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserStatsRepository;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final UserStatsRepository userStatsRepository;

    @Override
    public UserStats save(UserStats userStats) {
        log.debug("Saving UserStats for user with id: {}", userStats.getUserId());
        UserStats savedUserStats = userStatsRepository.save(userStats);
        log.debug("Saved UserStats for user with id: {}", savedUserStats.getUserId());
        return savedUserStats;
    }

    @Override
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
    public void incrementStats(String userId, Consumer<UserStats> updater) {
        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> UserStats.builder().userId(userId).build());
        updater.accept(stats);
        userStatsRepository.save(stats);
    }
}
