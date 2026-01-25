package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class UserGenerateNewMessageEvent extends AbstractEvent {

    @Builder
    public UserGenerateNewMessageEvent(String userId) {
        super(userId);
    }
}
