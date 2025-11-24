package com.bsu.cvbuilder.entity.resume.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
public class ResumeMetadata {
    @Builder.Default
    private Integer atsScore = 0; // 0-100
    
    @Builder.Default
    private Integer completenessScore = 0; // 0-100
    
    @Builder.Default
    private Integer readabilityScore = 0; // 0-100
    
    @Builder.Default
    private Integer viewCount = 0;
    
    @Builder.Default
    private Integer downloadCount = 0;
    
    @Builder.Default
    private Integer shareCount = 0;
    
    @Builder.Default
    private Integer aiImprovementCount = 0;
    
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime lastImprovedAt;
    
    @Builder.Default
    private AIUsageStats aiUsageStats = new AIUsageStats();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIUsageStats {
        @Builder.Default
        private Integer textImprovements = 0;
        
        @Builder.Default
        private Integer skillSuggestions = 0;
        
        @Builder.Default
        private Integer atsOptimizations = 0;
        
        @Builder.Default
        private Integer jobMatches = 0;
        
        @Builder.Default
        private LocalDateTime lastAIActionAt = LocalDateTime.now();
    }
}