package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.BlackListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/management")
@RequiredArgsConstructor
public class ManagementController {

    private final AuthService authService;
    private final BlackListService blackListService;

    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAdmin(@Valid @RequestBody RegisterAuthDto authRequest) {
        authService.registerWithRole(authRequest, UserProfile.Role.ADMIN);
    }

    @PostMapping("/ban/{userId}")
    public void banUser(@PathVariable String userId) {
        blackListService.banUser(userId);
    }
}
