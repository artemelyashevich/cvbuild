package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

public class UserCreatedEvent extends AbstractEvent {

    @Getter
    private final transient UserProfile user;

    @Builder
    public UserCreatedEvent(UserProfile user) {
        super(user.getId());
        this.user = user;
        setData(Map.of("status", "success"));
    }
}
