package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.history.HistoryEventsDto;
import com.bsu.cvbuilder.domain.entity.History;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoryService {

    Page<History> findAll(Pageable pageable);

    History findByUserId(String userId, Pageable pageRequest);

    void save(AbstractEvent event);

    void deleteAllByUserId(String id);

    HistoryEventsDto findByCurrentUser(Integer page, Integer size);
}
