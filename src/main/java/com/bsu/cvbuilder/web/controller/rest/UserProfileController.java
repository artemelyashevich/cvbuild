package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.web.dto.user.UserResponseDto;
import com.bsu.cvbuilder.web.mapper.impl.UserResponseDtoMapper;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private static final UserResponseDtoMapper responseMapper = Mappers.getMapper(UserResponseDtoMapper.class);

    private final UserProfileService userProfileService;
    private final SecurityService securityService;

    @GetMapping("/{userId}")
    public UserResponseDto findUserById(@PathVariable String userId) {
        return responseMapper.toDto(userProfileService.findById(userId));
    }

    @GetMapping("/current")
    public UserResponseDto findCurrentUser() {
        return responseMapper.toDto(securityService.findCurrentUser());
    }
}