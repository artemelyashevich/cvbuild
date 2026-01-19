package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);

    AuthResponse register(RegisterAuthDto authRequest);

    AuthResponse refreshToken(RefreshRequest refreshRequest);
}
