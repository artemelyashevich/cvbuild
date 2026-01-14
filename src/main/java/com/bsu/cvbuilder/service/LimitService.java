package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.limit.AiLimit;

import java.util.List;

public interface LimitService {

    List<AiLimit> findAllLimits();

    List<AiLimit> findActiveLimits();

    AiLimit createLimit(AiLimit limit);
}
