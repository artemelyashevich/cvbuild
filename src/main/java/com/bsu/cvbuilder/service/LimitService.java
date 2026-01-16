package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.annotation.limit.LimitType;

public interface LimitService {

    void check(String userId, LimitType type, int capacity);
}
