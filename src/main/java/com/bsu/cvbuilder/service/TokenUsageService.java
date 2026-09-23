package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.ai.TokenUsageDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;

public interface TokenUsageService {

    TokenUsageDto getUsage(UserProfile user);

    void checkLimit(UserProfile user);
}
