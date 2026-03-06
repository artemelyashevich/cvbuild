package com.bsu.cvbuilder.domain.event;

public class FormAiGenerationEvent extends AbstractEvent{
    public FormAiGenerationEvent(String userId) {
        super(userId);
    }
}
