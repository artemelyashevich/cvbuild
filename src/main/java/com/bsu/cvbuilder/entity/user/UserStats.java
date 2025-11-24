package com.bsu.cvbuilder.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStats {
    @Builder.Default
    private Integer resumesCreated = 0;
    
    @Builder.Default
    private Integer resumesPublished = 0;
    
    @Builder.Default
    private Integer aiRequestsUsed = 0;
    
    @Builder.Default
    private Integer jobAnalysesUsed = 0;
    
    @Builder.Default
    private Integer totalDownloads = 0;
    
    @Builder.Default
    private Integer totalViews = 0;

    private Integer totalMonthsSubscribed = 0;
    
    @Builder.Default
    private MonthlyUsage currentMonthUsage = new MonthlyUsage();

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUsage {
        @Builder.Default
        private Integer aiRequests = 0;

        @Builder.Default
        private Integer jobAnalyses = 0;

        @Builder.Default
        private Integer resumesCreated = 0;

        private LocalDateTime periodStart = LocalDateTime.now().withDayOfMonth(1);
    }
}
