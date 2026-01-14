package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.user.UserStats;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserStatsRepository extends MongoRepository<UserStats, String> {

    Optional<UserStats> findByUserId(String userId);
}
