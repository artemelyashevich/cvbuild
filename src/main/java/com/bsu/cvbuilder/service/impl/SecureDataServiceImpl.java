package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
        log.debug("Preparing secure data for user: {}", user.getLogin());

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
    public boolean checkCredsAndIf2faIsRequire(UserProfile userProfile, AuthRequest authRequest) {
        SecureData secureData = findByUserId(userProfile.getId());

        if (!passwordEncoder.matches(authRequest.password(), secureData.getPassword())) {
            log.warn("Password mismatch for user: {}", userProfile.getLogin());
            throw new AppException("Invalid credentials", 401);
        }
        if (secureData.getSecondAuthPhaseRequire() == null) {
            secureData.setSecondAuthPhaseRequire(false);
            secureDataRepository.save(secureData);
        }
        return secureData.getSecondAuthPhaseRequire();
    }

    @Override
    public SecureData findByUserId(String id) {
        return secureDataRepository.findByUserId(id)
                .orElseThrow(() -> new AppException("Login via outh2.0 with this email" + id, 401));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateNewEvent(String userId, SecureEvent secureEvent) {
        SecureData secureData = findByUserId(userId);

        List<LocalDateTime> eventDates = Optional.ofNullable(secureData.getSecureEvents())
                .map(events -> events.get(secureEvent))
                .orElse(Collections.emptyList());

        if (eventDates.isEmpty()) {
            return;
        }

        LocalDateTime lastDate = eventDates.stream()
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> {
                    log.error("No event found for user {}", userId);
                    return new AppException("Security data not found for user: " + userId, 401);
                });

        Duration offset = secureEvent.getDuration();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAllowedTime = lastDate.plus(offset);

        if (now.isBefore(nextAllowedTime)) {
            long hoursRemaining = ChronoUnit.HOURS.between(now, nextAllowedTime);
            throw new AppException(
                    "Limit reached. You can perform this action in %d hours".formatted(hoursRemaining),
                    401
            );
        }
    }

    @Override
    public void update(String userId, SecureEvent secureEvent, Consumer<SecureData> updater) {
        SecureData secureData = findByUserId(userId);
        updater.accept(secureData);
        secureDataRepository.save(secureData);
    }

    @Override
    public void deleteByUserId(String id) {
        log.debug("Attempting delete secure data for user with id: {}", id);
        secureDataRepository.deleteByUserId(id);
        log.info("Deleted secure data for user with id: {}", id);
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
