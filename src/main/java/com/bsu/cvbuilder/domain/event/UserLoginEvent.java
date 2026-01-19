package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginEvent extends AbstractEvent {

    private final String userId;

    private final UserProfile userProfile;

    public UserLoginEvent(String userId, UserProfile userProfile) {
        super(userId);
        this.userId = userId;
        this.userProfile = userProfile;
    }
}
