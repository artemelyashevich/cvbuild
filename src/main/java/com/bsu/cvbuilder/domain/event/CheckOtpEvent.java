package com.bsu.cvbuilder.domain.event;

public class CheckOtpEvent extends AbstractEvent {

    public CheckOtpEvent(String userId) {
        super(userId);
    }
}
