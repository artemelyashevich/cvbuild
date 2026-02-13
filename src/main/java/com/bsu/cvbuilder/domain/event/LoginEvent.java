package com.bsu.cvbuilder.domain.event;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginEvent extends AbstractEvent {

    private final transient UserProfile userProfile;

    @Builder
    public LoginEvent(String userId, UserProfile userProfile) {
        super(userId);
        this.userProfile = userProfile;
    }
}
