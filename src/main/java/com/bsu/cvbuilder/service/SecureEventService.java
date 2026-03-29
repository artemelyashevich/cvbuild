package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.SecureEvent;

public interface SecureEventService {

    void handleEvent(SecureEvent event, Object dto);
}
