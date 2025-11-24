package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ResumeRepository;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final SecurityService securityService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resume::user", key = "result.get(0).userId")
    public List<Resume> findAll() {
        log.debug("Attempting find all resumes by current user");

        var currentUser = securityService.findCurrentUser();

        var resumes = resumeRepository.findAllByUserId(currentUser.getId());

        log.info("Found {} resumes by current user", resumes.size());
        return resumes;
    }

    @Override
    @Cacheable(value = "resume::id", key = "#id")
    public Resume findById(String id) {
        log.debug("Attempting find resume by id {}", id);

        var resume = resumeRepository.findById(id).orElseThrow(() -> {
            var message = "Resume with id: %s not found".formatted(id);
            log.error(message);
            return new AppException(message, 404);
        });

        log.info("Found resume {}", resume);
        return resume;
    }

    @Override
    @Caching(put = {
            @CachePut(value = "resume::id", key = "#result.id")
    })
    @Transactional
    public Resume create(Resume resume) {
        log.debug("Attempting to create resume {}", resume);
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "resume::id", key = "#resume.id"),
                    @CacheEvict(value = "resume::user", key = "#resume.userId")
            }
    )
    public Resume update(Resume resume) {
        return null;
    }
}
