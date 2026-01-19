package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import lombok.Builder;
import lombok.Getter;

@Builder
public class UserCreatedEvent extends AbstractEvent {

    @Getter
    private final UserProfile user;

    public UserCreatedEvent(UserProfile user) {
        super(user.getId());
        this.user = user;
    }
}
