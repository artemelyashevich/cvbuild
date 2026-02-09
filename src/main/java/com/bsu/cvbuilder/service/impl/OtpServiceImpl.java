package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String OTP_KEY = "otp:email:";
    private static final String ATTEMPTS_KEY = "otp:attempts:";
    private static final String BLOCKED_KEY = "otp:blocked:";

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationProperties applicationProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String create(UserProfile userProfile) {
        String email = userProfile.getEmail();
        validateEmail(email);

        if (redisTemplate.hasKey(BLOCKED_KEY + email)) { // NOSONAR
            log.warn("OTP generation blocked for user: {}", email);
            throw new AppException("Too many failed attempts. Please try again in 15 minutes", 429);
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        redisTemplate.opsForValue().set(OTP_KEY + email, otp,
                Duration.ofSeconds(applicationProperties.getCache().getVerification()));
        redisTemplate.delete(ATTEMPTS_KEY + email);

        log.info("New OTP created for user: {}", email);
        return otp;
    }

    @Override
    public boolean validateOtp(UserProfile userProfile, String otp) {
        String email = userProfile.getEmail();
        validateEmail(email);

        if (redisTemplate.hasKey(BLOCKED_KEY + email)) { // NOSONAR
            throw new AppException("Your account is temporarily blocked from OTP verification", 429);
        }

        String storedOtp = redisTemplate.opsForValue().get(OTP_KEY + email);
        if (storedOtp == null) {
            throw new AppException("OTP expired or never requested", 400);
        }

        if (storedOtp.equals(otp)) {
            clearOtpData(email);
            return true;
        }

        handleFailedAttempt(email);
        return false;
    }

    private void handleFailedAttempt(String email) {
        String key = ATTEMPTS_KEY + email;
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(30));
        }

        log.warn("Failed OTP attempt #{} for user: {}", attempts, email);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            blockUser(email);
        }
    }

    private void blockUser(String email) {
        log.error("Blocking user {} due to too many failed OTP attempts", email);

        redisTemplate.opsForValue().set(BLOCKED_KEY + email, "blocked", BLOCK_DURATION);

        clearOtpData(email);

        throw new AppException("Too many failed attempts. You are blocked for " + BLOCK_DURATION.toMinutes() + " minutes", 429);
    }

    private void clearOtpData(String email) {
        redisTemplate.delete(List.of(OTP_KEY + email, ATTEMPTS_KEY + email));
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException("User email is missing", 400);
        }
    }
}