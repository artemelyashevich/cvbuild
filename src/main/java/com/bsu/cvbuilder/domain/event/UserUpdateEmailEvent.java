package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

public class UserUpdateEmailEvent extends AbstractEvent {
    @Getter
    private final transient UserProfile user;

    @Builder
    public UserUpdateEmailEvent(UserProfile user) {
        super(user.getId());
        this.user = user;
    }
}
