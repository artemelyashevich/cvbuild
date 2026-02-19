package com.bsu.cvbuilder.domain.event;

public class AiExtractorEvent extends AbstractEvent {
    public AiExtractorEvent(String userId) {
        super(userId);
    }
}
