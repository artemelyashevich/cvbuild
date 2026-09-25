package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ResumeService {

    Resume save(Resume resume);

    Page<Resume> findAll(Pageable pageable);

    Resume findByChatId(UUID chatId);

    /**
     * @throws com.bsu.cvbuilder.exception.AppException 404 when missing, 403 when the resume belongs to another user
     */
    Resume findById(String id);

    /**
     * Like {@link #findById(String)}, but empty instead of 404 when the resume does not exist.
     */
    Optional<Resume> tryFindById(String id);

    Resume update(String resumeId, UpdateResumeRequest updateResumeRequest);
}
