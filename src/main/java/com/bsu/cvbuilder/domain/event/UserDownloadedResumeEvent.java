package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class UserDownloadedResumeEvent extends AbstractEvent {

    @Builder
    public UserDownloadedResumeEvent(String userId) {
        super(userId);
    }
}
