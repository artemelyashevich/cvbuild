package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEventListener {

    private final HistoryService historyService;

    @Async
    @EventListener
    public void storeEvent(AbstractEvent event) {
        log.debug("Storing event {}", event);

        historyService.save(event);
        log.info("Event stored {}", event);
    }
}
