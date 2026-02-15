package com.bsu.cvbuilder.domain.dto.ai;

public record StepAnalysisResult(
    boolean completed,
    String missingInfo,
    String reasoning
) {}