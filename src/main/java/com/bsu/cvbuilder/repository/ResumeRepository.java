package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.resume.ResumeData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<ResumeData, String> {
    Optional<ResumeData> findByChatId(String id);
}
