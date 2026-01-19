package com.bsu.cvbuilder.domain.event;

import lombok.Builder;
import lombok.Getter;

@Builder
public class UserGenerateNewMessageEvent extends AbstractEvent {

    @Getter
    private final String userId;

    public UserGenerateNewMessageEvent(String userId) {
        super(userId);
        this.userId = userId;
    }
}
