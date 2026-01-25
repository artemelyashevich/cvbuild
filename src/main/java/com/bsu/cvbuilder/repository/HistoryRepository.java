package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.history.History;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface HistoryRepository extends MongoRepository<History, String> {

    Optional<History> findByUserId(String userId);
}
