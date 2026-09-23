package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.ai.TokenUsageDto;
import com.bsu.cvbuilder.domain.entity.AiLimit;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.TokenUsageService;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenUsageServiceImpl implements TokenUsageService {

    private static final String FREE_PLAN = "FREE";

    private final UserStatsService userStatsService;
    private final ApplicationProperties applicationProperties;

    @Override
    public TokenUsageDto getUsage(UserProfile user) {
        YearMonth currentMonth = YearMonth.now();
        UserStats.MonthlyUsage monthly = userStatsService.findByUserId(user.getId()).getCurrentMonthUsage();

        boolean isCurrentPeriod = monthly != null && monthly.getPeriodStart() != null
                && YearMonth.from(monthly.getPeriodStart()).equals(currentMonth);
        long promptTokens = isCurrentPeriod ? Objects.requireNonNullElse(monthly.getPromptTokens(), 0L) : 0;
        long completionTokens = isCurrentPeriod ? Objects.requireNonNullElse(monthly.getCompletionTokens(), 0L) : 0;
        long used = promptTokens + completionTokens;

        Optional<AiLimit> activeLimit = findActiveLimit(user);
        long limit = activeLimit.map(AiLimit::getMonthlyTokens)
                .orElse(applicationProperties.getTokens().getFreeMonthlyLimit());

        return new TokenUsageDto(
                activeLimit.map(AiLimit::getName).orElse(FREE_PLAN),
                promptTokens,
                completionTokens,
                used,
                limit,
                Math.max(0, limit - used),
                currentMonth.atDay(1).atStartOfDay(),
                currentMonth.plusMonths(1).atDay(1).atStartOfDay()
        );
    }

    @Override
    public void checkLimit(UserProfile user) {
        if (!applicationProperties.getTokens().isEnforce()) {
            return;
        }
        TokenUsageDto usage = getUsage(user);
        if (usage.remaining() <= 0) {
            log.warn("User {} exhausted monthly token limit {} ({})", user.getId(), usage.limit(), usage.plan());
            throw new AppException("Monthly token limit exceeded. Resets at " + usage.resetAt(), 429);
        }
    }

    private static Optional<AiLimit> findActiveLimit(UserProfile user) {
        LocalDate today = LocalDate.now();
        return Optional.ofNullable(user.getAiLimits()).stream()
                .flatMap(List::stream)
                .filter(limit -> limit.getMonthlyTokens() != null)
                .filter(limit -> limit.getAppliedAt() == null || !today.isBefore(limit.getAppliedAt()))
                .filter(limit -> limit.getAppliedFor() == null || !today.isAfter(limit.getAppliedFor()))
                .max(Comparator.comparing(AiLimit::getMonthlyTokens));
    }
}
