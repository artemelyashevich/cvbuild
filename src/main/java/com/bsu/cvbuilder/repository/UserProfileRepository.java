package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<UserProfile> findByLogin(String login);
}
