package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.exception.AuthTokenException;
import com.bsu.cvbuilder.service.BlackListService;
import com.bsu.cvbuilder.service.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final ApplicationProperties applicationProperties;
    private final BlackListService blackListService;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(applicationProperties.getSecurity().getAccessSecret().getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(applicationProperties.getSecurity().getRefreshSecret().getBytes());
    }

    @Override
    public String extractLogin(String token, TokenType tokenType) {
        return extractClaim(token, tokenType, Claims::getSubject);
    }

    @Override
    public UserProfile.Role extractRole(String token, TokenType tokenType) {
        String role = extractClaim(token, tokenType, claims -> claims.get("role", String.class));
        return UserProfile.Role.valueOf(role.toUpperCase());
    }

    @Override
    public String generateToken(UserProfile user, TokenType tokenType) {
        long lifetime = getLifetime(tokenType);
        SecretKey key = getKey(tokenType);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + lifetime);

        return Jwts.builder()
                .setClaims(Map.of("role", user.getRole().name()))
                .setSubject(user.getLogin())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public void validateToken(String token, TokenType tokenType) {
        Boolean isBlacklisted = blackListService.validate(token);
        if (isBlacklisted) { // NOSONAR
            throw new AppException("This token is banned", 401);
        }
        getClaims(token, tokenType);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, TokenType.ACCESS, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, TokenType tokenType, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaims(token, tokenType);
        return claimsResolver.apply(claims);
    }

    private Claims getClaims(String token, TokenType tokenType) {
        if (token == null || token.isBlank()) {
            throw new AppException("Token is empty", 401);
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey(tokenType))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new AuthTokenException("Token expired", true);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw new AppException("Invalid token: " + e.getMessage(), 401);
        }
    }

    private SecretKey getKey(TokenType tokenType) {
        return tokenType == TokenType.ACCESS ? accessKey : refreshKey;
    }

    private long getLifetime(TokenType tokenType) {
        String lifetimeStr = (tokenType == TokenType.ACCESS)
                ? applicationProperties.getSecurity().getAccessLifetime()
                : applicationProperties.getSecurity().getRefreshLifetime();
        return Long.parseLong(lifetimeStr);
    }
}