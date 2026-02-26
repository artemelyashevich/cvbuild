package com.bsu.cvbuilder.domain.event;

public class CreateChatEvent extends AbstractEvent{
    public CreateChatEvent(String userId) {
        super(userId);
    }
}
