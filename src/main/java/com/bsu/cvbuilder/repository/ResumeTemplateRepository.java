package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ResumeTemplateRepository extends MongoRepository<ResumeTemplate, String> {
    Optional<ResumeTemplate> findByName(String name);
    boolean existsByName(String name);
}
