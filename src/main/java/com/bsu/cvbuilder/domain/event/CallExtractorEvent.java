package com.bsu.cvbuilder.domain.event;

public class CallExtractorEvent extends AbstractEvent {
    public CallExtractorEvent(String userId) {
        super(userId);
    }
}
