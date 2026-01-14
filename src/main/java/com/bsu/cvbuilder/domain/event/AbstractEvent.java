package com.bsu.cvbuilder.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public abstract class AbstractEvent extends ApplicationEvent {

    @Getter
    private final String userId;

    public AbstractEvent(String userId) {
        super(new Object());
        this.userId = userId;
    }
}
