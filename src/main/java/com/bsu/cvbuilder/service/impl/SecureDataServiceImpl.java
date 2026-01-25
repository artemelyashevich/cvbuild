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

@Slf4j
@Service
@RequiredArgsConstructor
public class SecureDataServiceImpl implements SecureDataService {

    private final JwtService jwtService;
    private final ApplicationProperties applicationProperties;
    private final SecureDataRepository secureDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SecureData prepareData(UserProfile user) {
        var secureData = secureDataRepository.findByUserId(user.getId()).orElse(
                SecureData.builder()
                        .userId(user.getId())
                        .refreshTokenEncoded(SecretDecodeUtil.encode(
                                jwtService.generateToken(user, TokenType.REFRESH),
                                applicationProperties.getSecurity().getDecodeSignature()))
                        .build()
        );

        var refreshToken = jwtService.generateToken(user, TokenType.REFRESH);
        var encodedToken = SecretDecodeUtil.encode(
                refreshToken,
                applicationProperties.getSecurity().getDecodeSignature()
        );

        if (secureData.getRefreshTokenEncoded() == null) {
            secureData.setRefreshTokenEncoded(encodedToken);
        } else {
            try {
                String decryptedToken = SecretDecodeUtil.decode(
                        secureData.getRefreshTokenEncoded(),
                        applicationProperties.getSecurity().getDecodeSignature()
                );
                jwtService.validateToken(decryptedToken, TokenType.REFRESH);
            } catch (AppException | AuthTokenException e) {
                secureData.setRefreshTokenEncoded(encodedToken);
                secureDataRepository.save(secureData);
            }
        }

        return secureDataRepository.save(secureData);
    }

    @Override
    public void checkData(UserProfile userProfile, AuthRequest authRequest) {
        var secureData = secureDataRepository.findByUserId(userProfile.getId()).orElseThrow(
                () -> new AppException("Invalid user profile", 401)
        );
        if (!passwordEncoder.matches(authRequest.password(), secureData.getPassword())) {
            log.debug("Password does not match stored value, email: {}", authRequest.email());
            throw new AppException("Password mismatch", 401);
        }
    }
}
