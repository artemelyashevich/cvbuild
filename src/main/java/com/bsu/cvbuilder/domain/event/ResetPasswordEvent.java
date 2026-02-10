package com.bsu.cvbuilder.domain.event;

public class ResetPasswordEvent extends AbstractEvent{
    public ResetPasswordEvent(String userId) {
        super(userId);
    }
}
