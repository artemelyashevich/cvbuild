package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    @GetMapping("/user/{id}")
    public UserStats findByUserId(@PathVariable("id") String userId) {
        return userStatsService.findByUserId(userId);
    }
}
