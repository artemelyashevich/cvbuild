package com.bsu.cvbuilder.domain.event;

public class CallAtsEvent extends AbstractEvent{
    public CallAtsEvent(String userId) {
        super(userId);
    }
}
