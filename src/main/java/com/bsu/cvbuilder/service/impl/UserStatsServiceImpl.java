package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.user.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserStatsRepository;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private static final String CACHE_ID = "user_id_stat";

    private final UserStatsRepository userStatsRepository;

    @Override
    @Caching(put = {
            @CachePut(value = CACHE_ID, key = "#userStats.userId")
    })
    public UserStats save(UserStats userStats) {
        log.debug("Saving UserStats for user with id: {}", userStats.getUserId());
        UserStats savedUserStats = userStatsRepository.save(userStats);
        log.debug("Saved UserStats for user with id: {}", savedUserStats.getUserId());
        return savedUserStats;
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
    public void incrementStats(String userId, Consumer<UserStats> updater) {
        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> UserStats.builder().userId(userId).build());
        updater.accept(stats);
        userStatsRepository.save(stats);
    }
}
