package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.web.dto.user.UpdateUserRequest;
import com.bsu.cvbuilder.web.dto.user.UserResponseDto;
import com.bsu.cvbuilder.web.mapper.impl.UserResponseDtoMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User Profile")
@RestController
@RequestMapping("/api/v1/user-profile")
@SecurityRequirement(name = "bearerAuth")
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

    @PatchMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto update(@Validated @RequestBody UpdateUserRequest request) {
        var profile = securityService.findCurrentUser();
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmail(request.getEmail());
        return responseMapper.toDto(userProfileService.update(profile));
    }

    @PostMapping("/avatar/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto uploadAvatar(@PathVariable String userId, @RequestParam("file") MultipartFile file) {
        return responseMapper.toDto(userProfileService.uploadAvatar(file, userId));
    }
}
