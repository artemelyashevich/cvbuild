package com.bsu.cvbuilder.domain.event;

public class SetPasswordEvent extends AbstractEvent {
    public SetPasswordEvent(String userId) {
        super(userId);
    }
}
