package com.bsu.cvbuilder.domain.event;

public class VerifyEmailRequestEvent extends AbstractEvent{
    public VerifyEmailRequestEvent(String userId) {
        super(userId);
    }
}
