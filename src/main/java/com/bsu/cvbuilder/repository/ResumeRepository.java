package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.resume.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResumeRepository extends MongoRepository<Resume, String> {

    List<Resume> findAllByUserId(String id);
}
