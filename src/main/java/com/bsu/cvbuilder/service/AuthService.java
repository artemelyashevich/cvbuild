package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);

    AuthResponse register(AuthRequest authRequest);
}
