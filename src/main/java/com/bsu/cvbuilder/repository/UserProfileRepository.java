package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.user.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<UserProfile> findByLogin(String login);
}
