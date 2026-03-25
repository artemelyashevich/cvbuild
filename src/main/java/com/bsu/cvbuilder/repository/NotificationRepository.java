package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Optional<Notification> findByUuid(UUID uuid);
}
