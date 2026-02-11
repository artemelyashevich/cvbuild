package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.UserStats;

import java.util.function.Consumer;

public interface UserStatsService {

    UserStats save(UserStats userStats);

    UserStats findByUserId(String id);

    void incrementStats(String userId, Consumer<UserStats> updater);
}
