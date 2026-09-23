package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.ai.TokenUsageDto;
import com.bsu.cvbuilder.domain.entity.AiLimit;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.UserStatsService;
import com.bsu.cvbuilder.service.impl.TokenUsageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenUsageServiceImplTest {

    private static final String USER_ID = "user-1";
    private static final long FREE_LIMIT = 1000;

    @Mock
    private UserStatsService userStatsService;

    private ApplicationProperties.Tokens tokens;
    private TokenUsageServiceImpl service;

    @BeforeEach
    void setUp() {
        tokens = new ApplicationProperties.Tokens();
        tokens.setFreeMonthlyLimit(FREE_LIMIT);
        var properties = new ApplicationProperties();
        properties.setTokens(tokens);
        service = new TokenUsageServiceImpl(userStatsService, properties);
    }

    private void statsWithUsage(LocalDateTime periodStart, long prompt, long completion) {
        when(userStatsService.findByUserId(USER_ID)).thenReturn(UserStats.builder()
                .userId(USER_ID)
                .currentMonthUsage(UserStats.MonthlyUsage.builder()
                        .periodStart(periodStart)
                        .promptTokens(prompt)
                        .completionTokens(completion)
                        .build())
                .build());
    }

    private static UserProfile user(AiLimit... limits) {
        return UserProfile.builder().id(USER_ID).aiLimits(new ArrayList<>(List.of(limits))).build();
    }

    private static AiLimit limit(String name, long tokens, LocalDate from, LocalDate to) {
        return AiLimit.builder().name(name).monthlyTokens(tokens).appliedAt(from).appliedFor(to).build();
    }

    private static LocalDateTime thisMonth() {
        return YearMonth.now().atDay(1).atStartOfDay();
    }

    @Test
    @DisplayName("getUsage: free plan uses configured limit and current month counters")
    void getUsage_NoActiveLimit_ReturnsFreePlan() {
        statsWithUsage(thisMonth(), 300, 200);

        TokenUsageDto usage = service.getUsage(user());

        assertAll(
                () -> assertEquals("FREE", usage.plan()),
                () -> assertEquals(500, usage.used()),
                () -> assertEquals(FREE_LIMIT, usage.limit()),
                () -> assertEquals(500, usage.remaining()),
                () -> assertEquals(YearMonth.now().plusMonths(1).atDay(1).atStartOfDay(), usage.resetAt())
        );
    }

    @Test
    @DisplayName("getUsage: stats from a previous month count as zero usage")
    void getUsage_StalePeriod_ReturnsZeroUsed() {
        statsWithUsage(thisMonth().minusMonths(1), 300, 200);

        TokenUsageDto usage = service.getUsage(user());

        assertEquals(0, usage.used());
        assertEquals(FREE_LIMIT, usage.remaining());
    }

    @Test
    @DisplayName("getUsage: active AiLimit overrides free limit, expired and future ones are ignored")
    void getUsage_ActiveAiLimit_UsesItsQuota() {
        statsWithUsage(thisMonth(), 0, 0);
        LocalDate today = LocalDate.now();

        TokenUsageDto usage = service.getUsage(user(
                limit("PRO", 50_000, today.minusDays(1), today.plusDays(29)),
                limit("EXPIRED", 900_000, today.minusDays(60), today.minusDays(30)),
                limit("FUTURE", 900_000, today.plusDays(1), today.plusDays(31))
        ));

        assertEquals("PRO", usage.plan());
        assertEquals(50_000, usage.limit());
    }

    @Test
    @DisplayName("checkLimit: does nothing when enforcement is disabled")
    void checkLimit_EnforceDisabled_SkipsCheck() {
        service.checkLimit(user());

        verifyNoInteractions(userStatsService);
    }

    @Test
    @DisplayName("checkLimit: throws 429 when quota is exhausted and enforcement is enabled")
    void checkLimit_QuotaExhausted_Throws429() {
        tokens.setEnforce(true);
        statsWithUsage(thisMonth(), 600, 400);

        AppException ex = assertThrows(AppException.class, () -> service.checkLimit(user()));

        assertEquals(429, ex.getStatusCode());
    }
}
