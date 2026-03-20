package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import jakarta.validation.Valid;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);

    AuthResponse register(RegisterAuthDto authRequest);

    void registerWithRole(RegisterAuthDto authRequest, UserProfile.Role role);

    AuthResponse refreshToken(RefreshRequest refreshRequest);

    void logout();

    AuthResponse verify2fa(@Valid Verify2faRequest verify2faRequest);

    void verify2faRefresh(@Valid Verify2faRefreshRequest verify2faRefreshRequest);
}
