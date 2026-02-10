package com.bsu.cvbuilder.domain.event;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateResumeEvent extends AbstractEvent {

    @Builder
    public CreateResumeEvent(String userId) {
        super(userId);
    }
}
