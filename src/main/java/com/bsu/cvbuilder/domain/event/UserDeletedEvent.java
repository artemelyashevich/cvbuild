package com.bsu.cvbuilder.domain.event;


import lombok.Builder;

public class UserDeletedEvent extends AbstractEvent {

    @Builder
    public UserDeletedEvent(String userId) {
        super(userId);
    }
}
