package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.security.SecureData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecureDataRepository extends MongoRepository<SecureData, String> {

    Optional<SecureData> findByUserId(String userId);
}
