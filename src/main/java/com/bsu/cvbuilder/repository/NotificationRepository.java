package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.history.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
}
