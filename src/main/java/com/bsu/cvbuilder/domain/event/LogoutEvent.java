package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class LogoutEvent extends AbstractEvent {

    @Builder
    public LogoutEvent(String userId) {
        super(userId);
    }
}
