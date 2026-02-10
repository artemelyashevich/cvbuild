package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.history.History;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.HistoryRepository;
import com.bsu.cvbuilder.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final HistoryRepository historyRepository;

    @Override
    public Page<History> findAll(Pageable pageable) {
        log.debug("Attempting to fetch histories");
        Page<History> histories = historyRepository.findAll(pageable);
        log.info("Fetch histories returned {}", histories.getTotalElements());
        return histories;
    }

    @Override
    public History findByUserId(String userId) {
        log.debug("Attempting to find History by userId {}", userId);

        History history = historyRepository.findByUserId(userId).orElseThrow(() -> {
            String message = String.format("History not found for userId: %s", userId);
            log.debug(message);
            return new AppException(message, 404);
        });

        log.info("History found for userId {}", userId);
        return history;
    }

    @Override
    @Transactional
    public void save(AbstractEvent event) {
        log.debug("Attempting to save history for user {}", event.getUserId());

        History history = historyRepository.findByUserId(event.getUserId()).orElseGet(History::new);

        history.setUserId(event.getUserId());
        history.getEvents().put(LocalDateTime.now().toString(), event.getClass().getSimpleName());

        historyRepository.save(history);
        log.info("History for user {} saved", event.getUserId());
    }

    @Override
    public void deleteAllByUserId(String id) {
        log.debug("Attempting to delete history for user {}", id);
        historyRepository.deleteByUserId(id);
        log.info("History for user {} deleted", id);
    }
}
