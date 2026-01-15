package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.annotation.LimitType;
import com.bsu.cvbuilder.domain.entity.limit.AiLimit;

import java.util.List;

public interface LimitService {

    void check(String userId, LimitType type, int capacity);
}
