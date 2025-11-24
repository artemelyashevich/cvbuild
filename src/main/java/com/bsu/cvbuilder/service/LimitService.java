package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.limit.AiLimit;

import java.util.List;

public interface LimitService {

    List<AiLimit> findActiveLimits();

    AiLimit createLimit(AiLimit limit);
}
