package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.security.SecureDataCacheSingleton;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.util.LockUtil;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final LockService lockService;
    private final PasswordEncoder passwordEncoder;
    private final SecureDataCacheSingleton secureDataRequestCache;

    @Override
    @Transactional
    public SecureData prepareData(UserProfile user) {
        log.debug("Preparing secure data for user: {}", user.getLogin());

        SecureData secureData;

        try {
            secureData= findByUserId(user.getId());
        } catch (AppException e) {
            secureData = SecureData.builder()
                    .userId(user.getId())
                    .build();
        }

        if (isTokenValid(secureData.getRefreshTokenEncoded())) {
            return secureData;
        }

        log.info("Refresh token expired or missing for user {}, generating new one", user.getId());
        String newToken = jwtService.generateToken(user, TokenType.REFRESH);
        String encodedToken = encodeToken(newToken);

        secureData.setRefreshTokenEncoded(encodedToken);
        return saveAndCache(secureData);
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
            saveAndCache(secureData);
        }
        return secureData.getSecondAuthPhaseRequire();
    }

    @Override
    public SecureData findByUserId(String id) {
        if (secureDataRequestCache.getSecureData(id) != null) {
            return secureDataRequestCache.getSecureData(id);
        }
        SecureData secureData = secureDataRepository.findByUserId(id)
                .orElseThrow(() -> new AppException("User [SECURE] data not found", 404));
        secureDataRequestCache.set(id, secureData);
        return secureData;
    }

    public SecureData saveAndCache(SecureData secureData) {
        SecureData data = secureDataRepository.save(secureData);
        secureDataRequestCache.set(data.getUserId(), data);
        return data;
    }

    @Override
    public void deleteByUserId(String id) {
        log.debug("Attempting delete secure data for user with id: {}", id);
        secureDataRepository.deleteByUserId(id);
        secureDataRequestCache.clearCache(id);
        log.info("Deleted secure data for user with id: {}", id);
    }

    @Override
    public void performEvent(UserProfile profile, SecureEvent secureEvent) {
        SecureData secureData = findByUserId(profile.getId());
        switch (secureEvent){
            case verifyEmail -> {
                validateNewEvent(profile.getId(), secureEvent);
                secureData.setEmailVerified(true);
                secureDataRepository.save(secureData);
            }
            default -> throw new AppException("Invalid event type", 500);
        }
    }

    @Override
    @Transactional
    public SecureData loadSecureData(SecureData secureData) {
        SecureData persistentData = secureDataRepository.findByUserId(secureData.getUserId())
                .orElseGet(() -> SecureData.builder().userId(secureData.getUserId()).build());
        if (StringUtils.hasText(secureData.getPassword())) {
            persistentData.setPassword(passwordEncoder.encode(secureData.getPassword()));
        }
        SecureData data = secureDataRepository.save(persistentData);
        return data;
    }

    private boolean isTokenValid(String encodedToken) {
        if (encodedToken == null) return false;

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
        return SecretDecodeUtil.encode(token, applicationProperties.getSecurity().getDecodeSignature());
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
                    400
            );
        }
    }

    @Override
    @Transactional
    public void update(String userId, Consumer<SecureData> updater) {
        lockService.withLock(LockUtil.SECURE_DATA.formatted(userId), ()-> {
            SecureData secureData = findByUserId(userId);
            updater.accept(secureData);
            SecureData updated = secureDataRepository.save(secureData);
            secureDataRequestCache.set(userId, updated);
            return secureData;
        });
        log.info("Updated secure data for user: {}", userId);
    }
}
