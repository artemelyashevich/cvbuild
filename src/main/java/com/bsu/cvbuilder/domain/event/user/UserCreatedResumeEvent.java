package com.bsu.cvbuilder.domain.event.user;

import com.bsu.cvbuilder.domain.event.AbstractEvent;
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
