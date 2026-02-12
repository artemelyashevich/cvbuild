package com.bsu.cvbuilder.domain.event;

import lombok.Builder;

public class DownloadResumeEvent extends AbstractEvent {

    @Builder
    public DownloadResumeEvent(String userId) {
        super(userId);
    }
}
