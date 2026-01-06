package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.AuthResponse;
import com.bsu.cvbuilder.dto.auth.AuthRequest;
import org.springframework.security.core.Authentication;

public interface AuthService {

    AuthResponse authenticate(Authentication authentication);

    AuthResponse register(AuthRequest authRequest);
}
