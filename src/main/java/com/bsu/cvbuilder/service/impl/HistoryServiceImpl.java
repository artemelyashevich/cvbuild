package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.dto.history.HistoryEventsDto;
import com.bsu.cvbuilder.domain.entity.History;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.HistoryRepository;
import com.bsu.cvbuilder.service.HistoryService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.JsonHelper;
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
    private final SecurityService securityService;

    @Override
    public Page<History> findAll(Pageable pageable) {
        log.debug("Attempting to fetch histories");
        Page<History> histories = historyRepository.findAll(pageable);
        log.info("Fetch histories returned {}", histories.getTotalElements());
        return histories;
    }

    @Override
    public History findByUserId(String userId, Pageable pageRequest) {
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
    @Monitored(value = "storing_history", context = "internal")
    public void save(AbstractEvent event) {
        log.debug("Attempting to save history for user {}", event.getUserId());

        History history = historyRepository.findByUserId(event.getUserId()).orElseGet(History::new);

        history.setUserId(event.getUserId());
        history.getEvents().put(LocalDateTime.now().toString(), JsonHelper.toJson(event.getData()));

        historyRepository.save(history);
        log.info("History for user {} saved", event.getUserId());
    }

    @Override
    public void deleteAllByUserId(String id) {
        log.debug("Attempting to delete history for user {}", id);
        historyRepository.deleteByUserId(id);
        log.info("History for user {} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryEventsDto findByCurrentUser(Integer page, Integer size) {
        log.debug("Attempting find histories for current user");
        UserProfile userProfile = securityService.findCurrentUser();
        HistoryEventsDto history = historyRepository.findByUserId(userProfile.getId(), page, size);
        if (history == null) {
            log.info("History for user {} not found", userProfile.getLogin());
            throw new AppException("History not found for user", 404);
        }
        log.info("Found history for current user {}", userProfile.getLogin());
        return history;
    }
}
