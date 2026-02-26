package com.bsu.cvbuilder.domain.event;

public class CallAnalyzerEvent extends AbstractEvent{
    public CallAnalyzerEvent(String userId) {
        super(userId);
    }
}
