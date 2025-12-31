package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.TokenType;
import com.bsu.cvbuilder.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final ApplicationProperties applicationProperties;

    @Override
    public String extractLogin(String token, TokenType tokenType) {
        var claims = switch (tokenType) {
            case ACCESS -> getClaimsFromToken(token, applicationProperties.getSecurity().getAccessSecret());
            case REFRESH -> getClaimsFromToken(token, applicationProperties.getSecurity().getRefreshSecret());
        };
        return claims.getSubject();
    }

    @Override
    public UserProfile.Role extractRole(String token, TokenType tokenType) {
        var claims = switch (tokenType) {
            case ACCESS -> getClaimsFromToken(token, applicationProperties.getSecurity().getAccessSecret());
            case REFRESH -> getClaimsFromToken(token, applicationProperties.getSecurity().getRefreshSecret());
        };
        return UserProfile.Role.valueOf(claims.get("role", String.class).toUpperCase());
    }

    @Override
    public String generateToken(UserProfile user, TokenType tokenType) {
        return switch (tokenType) {
            case ACCESS -> createToken(
                    user,
                    Long.parseLong(applicationProperties.getSecurity().getAccessLifetime()),
                    applicationProperties.getSecurity().getAccessSecret()
            );
            case REFRESH -> createToken(
                    user,
                    Long.parseLong(applicationProperties.getSecurity().getRefreshLifetime()),
                    applicationProperties.getSecurity().getRefreshSecret()
            );
        };
    }

    @Override
    public void validateToken(String token, TokenType tokenType) {
        if (token == null || token.isBlank()) {
            throw new AppException("Token is empty", 401);
        }

        String secret = switch (tokenType) {
            case ACCESS -> applicationProperties.getSecurity().getAccessSecret();
            case REFRESH -> applicationProperties.getSecurity().getRefreshSecret();
        };

        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token);

        } catch (ExpiredJwtException e) {
            throw new AppException("Token expired", 401);
        } catch (SignatureException e) {
            throw new AppException("Invalid token signature", 401);
        } catch (MalformedJwtException e) {
            throw new AppException("Invalid token format", 401);
        } catch (UnsupportedJwtException e) {
            throw new AppException("Unsupported token", 401);
        } catch (IllegalArgumentException e) {
            throw new AppException("Token claims string is empty", 401);
        } catch (Exception e) {
            throw new AppException("Token validation error: " + e.getMessage(), 401);
        }
    }

    private Claims getClaimsFromToken(final String token, final String secret) {
        return Jwts
                .parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
                        return secret.getBytes();
                    }
                })
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private String createToken(final UserProfile userDetails, final Long tokenLifeTime, String secret) {
        var issuedAt = new Date();
        var expirationDate = new Date(issuedAt.getTime() + tokenLifeTime);

        return Jwts.builder()
                .setClaims(
                        Map.of(
                                "role",
                               userDetails.getRole().name()
                        )
                )
                .setSubject(userDetails.getEmail())
                .setIssuedAt(issuedAt)
                .setExpiration(expirationDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}
