package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.history.History;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistoryRepository extends MongoRepository<History, String> {
}
