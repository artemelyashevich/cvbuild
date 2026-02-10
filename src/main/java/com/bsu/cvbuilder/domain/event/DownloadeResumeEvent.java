package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class DownloadeResumeEvent extends AbstractEvent {

    @Builder
    public DownloadeResumeEvent(String userId) {
        super(userId);
    }
}
