package com.bsu.cvbuilder.domain.event;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

public abstract class AbstractEvent implements Serializable {

    @Getter
    @ToString.Include
    private final String userId;

    public AbstractEvent(String userId) {
        this.userId = userId;
    }
}
