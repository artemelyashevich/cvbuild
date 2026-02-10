package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.*;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);

    AuthResponse register(RegisterAuthDto authRequest);

    AuthResponse refreshToken(RefreshRequest refreshRequest);

    void logout();
}
