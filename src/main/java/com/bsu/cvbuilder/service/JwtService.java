package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;

import java.util.Date;

public interface JwtService {

    String extractLogin(String token, TokenType tokenType);

    UserProfile.Role extractRole(String token, TokenType tokenType);

    String generateToken(UserProfile user, TokenType tokenType);

    void validateToken(String token, TokenType tokenType);

    Date extractExpiration(String token);
}
