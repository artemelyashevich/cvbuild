package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.limit.AiLimit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LimitRepository extends MongoRepository<AiLimit, String> {
}
