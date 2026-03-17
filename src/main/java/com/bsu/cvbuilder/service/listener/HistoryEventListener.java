package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEventListener {

    private final HistoryService historyService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void storeEvent(AbstractEvent event) {
        historyService.save(event);
    }
}
