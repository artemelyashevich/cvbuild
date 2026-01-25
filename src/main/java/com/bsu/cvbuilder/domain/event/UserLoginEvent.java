package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserLoginEvent extends AbstractEvent {

    private final UserProfile userProfile;

    @Builder
    public UserLoginEvent(String userId, UserProfile userProfile) {
        super(userId);
        this.userProfile = userProfile;
    }
}
