package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.limit.AiLimit;
import com.bsu.cvbuilder.repository.LimitRepository;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.LimitService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import io.lettuce.core.Limit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final SecurityService securityService;
    private final LimitRepository limitRepository;
    private final UserProfileService userProfileService;

    @Override
    public List<AiLimit> findActiveLimits() {
        log.debug("Attempting to fetch limit list by current user");

        var user = securityService.findCurrentUser();

        var limits = user.getAiLimits();

        var activeLimits = limits.stream()
                .filter(limit -> limit.getAppliedFor().isAfter(LocalDate.now()))
                .toList();

        log.info("Found {} active limits for current user", limits.size());
        return activeLimits;
    }

    @Override
    @Transactional
    public AiLimit createLimit(AiLimit limit) {
        log.debug("Attempting to create limit by current user");
        var user = securityService.findCurrentUser();
        var newLimit = AiLimit.builder()
                .name(limit.getName())
                .description(limit.getDescription())
                .appliedAt(LocalDate.now())
                .appliedFor(limit.getAppliedFor() == null ? LocalDate.now().plusDays(30) : limit.getAppliedFor())
                .build();
        var createdLimit = limitRepository.save(newLimit);
        user.getAiLimits().add(createdLimit);
        userProfileService.update(user);
        log.info("Created limit by current user: {}", createdLimit);
        return createdLimit;
    }
}
