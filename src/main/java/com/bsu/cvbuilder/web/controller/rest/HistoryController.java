package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.entity.History;
import com.bsu.cvbuilder.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{userId}")
    public History findAllHistoryByUser(@PathVariable String userId) {
        return historyService.findByUserId(userId);
    }
}
