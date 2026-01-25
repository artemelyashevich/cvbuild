package com.bsu.cvbuilder.domain.event;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
public class UserCreatedResumeEvent extends AbstractEvent {

    @Builder
    public UserCreatedResumeEvent(String userId) {
        super(userId);
    }
}
