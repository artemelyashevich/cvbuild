package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.web.dto.user.UserResponseDto;
import com.bsu.cvbuilder.web.mapper.impl.UserResponseDtoMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "User Profile")
@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private static final UserResponseDtoMapper responseMapper = Mappers.getMapper(UserResponseDtoMapper.class);

    private final UserProfileService userProfileService;
    private final SecurityService securityService;
    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public UserResponseDto findUserById(@PathVariable String userId) {
        return responseMapper.toDto(userProfileService.findById(userId));
    }

    @GetMapping("/current")
    public UserResponseDto findCurrentUser() {
        notificationService.sendNotification(NotificationDto.builder()
                        .receiver(securityService.findCurrentUser().getLogin())
                        .engine(NotificationEngine.WS)
                        .parameters(Map.of("Message", "test"))
                .build());
        return responseMapper.toDto(securityService.findCurrentUser());
    }
}