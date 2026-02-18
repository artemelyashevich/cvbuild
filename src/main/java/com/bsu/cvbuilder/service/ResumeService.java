package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResumeService {

    Resume save(Resume resume);

    Page<Resume> findAll(Pageable pageable);

    Resume findByChatId(UUID chatId);

    Resume findById(String id);

    Resume update(String resumeId, UpdateResumeRequest updateResumeRequest);
}
