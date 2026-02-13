package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.dto.history.HistoryEventsDto;

public interface HistoryEventRepository {
    HistoryEventsDto findByUserId(String id, int page, int size);
}
