package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class UserLogoutEvent extends AbstractEvent {

    @Builder
    public UserLogoutEvent(String userId) {
        super(userId);
    }
}
