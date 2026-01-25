package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.history.History;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoryService {

    Page<History> findAll(Pageable pageable);

    History findByUserId(String userId);

    void save(AbstractEvent event);
}
