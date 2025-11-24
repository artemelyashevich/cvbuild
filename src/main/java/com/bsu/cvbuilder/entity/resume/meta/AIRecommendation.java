package com.bsu.cvbuilder.entity.resume.meta;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AIRecommendation {
    private String id;
    private String section;
    private String field;
    private String currentText;
    private String suggestedText;
    private String explanation;
    
    @Builder.Default
    private RecommendationType type = RecommendationType.IMPROVEMENT;
    
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
    
    @Builder.Default
    private Boolean isApplied = false;
    
    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;
    
    public enum RecommendationType {
        IMPROVEMENT,
        OPTIMIZATION,
        CORRECTION,
        COMPLETION
    }
    
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}