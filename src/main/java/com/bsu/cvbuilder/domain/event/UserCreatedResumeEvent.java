package com.bsu.cvbuilder.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreatedResumeEvent extends AbstractEvent {
    private final String userId;

    public UserCreatedResumeEvent(String userId) {
        super(userId);
        this.userId = userId;
    }
}
