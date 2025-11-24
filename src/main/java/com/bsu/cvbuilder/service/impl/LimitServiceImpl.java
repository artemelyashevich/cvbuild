package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.limit.AiLimit;
import com.bsu.cvbuilder.repository.LimitRepository;
import com.bsu.cvbuilder.service.LimitService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final SecurityService securityService;
    private final LimitRepository limitRepository;

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
    public AiLimit createLimit(AiLimit limit) {
        return null;
    }
}
