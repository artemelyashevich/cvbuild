package com.bsu.cvbuilder.domain.event.user;

import com.bsu.cvbuilder.domain.event.AbstractEvent;
import lombok.Builder;
import lombok.Getter;

@Builder
public class UserDownloadedResumeEvent extends AbstractEvent {

    @Getter
    private final String userId;

    public UserDownloadedResumeEvent(String userId) {
        super(userId);
        this.userId = userId;
    }
}
