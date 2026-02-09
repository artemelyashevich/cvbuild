package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.exception.AuthTokenException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecureDataServiceImpl implements SecureDataService {

    private final JwtService jwtService;
    private final ApplicationProperties applicationProperties;
    private final SecureDataRepository secureDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public SecureData prepareData(UserProfile user) {
        log.debug("Preparing secure data for user: {}", user.getId());

        SecureData secureData = secureDataRepository.findByUserId(user.getId())
                .orElseGet(() -> SecureData.builder().userId(user.getId()).build());

        if (isTokenValid(secureData.getRefreshTokenEncoded())) {
            return secureData;
        }

        log.info("Refresh token expired or missing for user {}, generating new one", user.getId());
        String newToken = jwtService.generateToken(user, TokenType.REFRESH);
        String encodedToken = encodeToken(newToken);

        secureData.setRefreshTokenEncoded(encodedToken);
        return secureDataRepository.save(secureData);
    }

    @Override
    public void checkData(UserProfile userProfile, AuthRequest authRequest) {
        SecureData secureData = findByUserId(userProfile.getId());

        if (!passwordEncoder.matches(authRequest.password(), secureData.getPassword())) {
            log.warn("Password mismatch for user: {}", userProfile.getEmail());
            throw new AppException("Invalid credentials", 401);
        }
    }

    @Override
    public SecureData findByUserId(String id) {
        return secureDataRepository.findByUserId(id)
                .orElseThrow(() -> new AppException("Security data not found for user: " + id, 401));
    }

    @Override
    @Transactional
    public void loadSecureData(SecureData secureData) {
        SecureData persistentData = secureDataRepository.findByUserId(secureData.getUserId())
                .orElseGet(() -> SecureData.builder().userId(secureData.getUserId()).build());

        persistentData.setPassword(passwordEncoder.encode(secureData.getPassword()));
        secureDataRepository.save(persistentData);
    }

    private boolean isTokenValid(String encodedToken) {
        if (encodedToken == null) {
            return false;
        }

        try {
            String decryptedToken = SecretDecodeUtil.decode(
                    encodedToken,
                    applicationProperties.getSecurity().getDecodeSignature()
            );
            jwtService.validateToken(decryptedToken, TokenType.REFRESH);
            return true;
        } catch (Exception e) {
            log.debug("Stored refresh token is invalid: {}", e.getMessage());
            return false;
        }
    }

    private String encodeToken(String token) {
        return SecretDecodeUtil.encode(
                token,
                applicationProperties.getSecurity().getDecodeSignature()
        );
    }
}
