package com.bsu.cvbuilder.service.provider;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

public final class LimitTestData {

    private LimitTestData() {}

    /**
     * Provides a stream of arguments for limit check scenarios.
     * Format: userId, LimitType, capacity, redisReturnValue, expectedException (bool)
     */
    public static Stream<Arguments> provideLimitCheckScenarios() {
        return Stream.of(
            // Success cases
            Arguments.of("user-1", LimitType.RESUME_DOWNLOAD, 10, 1L, false),
            Arguments.of("user-2", LimitType.AI_MESSAGE, 5, 5L, false),
            
            // Failure cases: -1 means already banned
            Arguments.of("user-banned", LimitType.RESUME_DOWNLOAD, 10, -1L, true),
            
            // Failure cases: -2 means just exceeded limit
            Arguments.of("user-exceeded", LimitType.RESUME_DOWNLOAD, 10, -2L, true)
        );
    }
}