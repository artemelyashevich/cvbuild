package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.SecurityProvider;
import com.bsu.cvbuilder.domain.dto.user.CurrentProfileDto;
import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserCreatedEvent;
import com.bsu.cvbuilder.domain.event.UserUpdateEmailEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.ImageService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String CACHE_ID = "user_id";
    private static final String CACHE_EMAIL = "user_email";
    private static final String CACHE_LOGIN = "user_login";

    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageService imageService;
    private final UserMapper userMapper;
    private final SecurityProvider securityProvider;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_EMAIL, key = "#email")
    public UserProfile findByEmail(String email) {
        log.debug("Finding user profile by email: {}", email);
        return userProfileRepository.findByEmail(email)
                .orElseThrow(notFound("email", email));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_LOGIN, key = "#login")
    public UserProfile findByLogin(String login) {
        log.debug("Finding user profile by login: {}", login);

        if (securityProvider != null && securityProvider.getUserProfile() != null) {
            if (securityProvider.getUserProfile().getLogin().equals(login)) {
                return securityProvider.getUserProfile();
            }
            if (securityProvider.getUserProfile().getEmail().equals(login)) {
                return securityProvider.getUserProfile();
            }
        }

        UserProfile userProfile = userProfileRepository.findByLogin(login)
                .or(() -> userProfileRepository.findByEmail(login))
                .orElseThrow(notFound("login/email", login));

        securityProvider.setUserProfile(userProfile);
        return userProfile;
    }

    @Override
    @Cacheable(value = CACHE_ID, key = "#id")
    public UserProfile findById(String id) {
        log.debug("Finding user profile by id: {}", id);
        return userProfileRepository.findById(id)
                .orElseThrow(notFound("id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsByEmail(String email) {
        return userProfileRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = CACHE_ID, key = "#result.id"),
            @CachePut(value = CACHE_EMAIL, key = "#result.email", condition = "#result.email != null"),
            @CachePut(value = CACHE_LOGIN, key = "#result.login", condition = "#result.login != null")
    })
    public UserProfile create(UserProfile userProfile) {
        log.debug("Creating user profile: {}", userProfile.getLogin());
        UserProfile saved = userProfileRepository.save(userProfile);
        eventPublisher.publishEvent(new UserCreatedEvent(saved));
        return saved;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ID, key = "#result.id"),
            @CacheEvict(value = CACHE_EMAIL, key = "#result.email", condition = "#result.email != null"),
            @CacheEvict(value = CACHE_LOGIN, key = "#result.login", condition = "#result.login != null")
    })
    public UserProfile login(String login) {
        log.debug("User login attempt: {}", login);

        UserProfile user = userProfileRepository.findByLogin(login)
                .or(() -> userProfileRepository.findByEmail(login))
                .orElseGet(() -> {
                    log.info("User not found, preparing new profile for: {}", login);
                    UserProfile userProfile = UserProfile.builder().login(login).build();
                    UserProfile saved = userProfileRepository.save(userProfile);
                    eventPublisher.publishEvent(new UserCreatedEvent(saved));
                    return saved;
                });

        user.setLastLogin(LocalDateTime.now());
        return userProfileRepository.save(user);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_ID, key = "#profile.id"),
            @CacheEvict(value = CACHE_EMAIL, key = "#result.email", condition = "#result.email != null"),
            @CacheEvict(value = CACHE_LOGIN, key = "#result.login", condition = "#result.login != null")
    })
    public UserProfile update(UserProfile profile) {
        log.debug("Updating user profile: {}", profile.getId());
        return transactionTemplate.execute(s -> {
            UserProfile existingUser = findById(profile.getId());
            if (userProfileRepository.existsByEmail(profile.getEmail()) && !existingUser.getEmail().equals(profile.getEmail())) {
                throw new AppException("User this such email already exists", 400);
            }
            userMapper.updateEntity(profile, existingUser);
            return userProfileRepository.save(existingUser);
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ID, key = "#id"),
            @CacheEvict(value = CACHE_EMAIL, key = "#result.email", condition = "#result.email != null"),
            @CacheEvict(value = CACHE_LOGIN, key = "#result.login", condition = "#result.login != null")
    })
    public UserProfile uploadAvatar(MultipartFile file, String id) {
        UserProfile userProfile = findById(id);

        ImageMetadata imageMetadata = imageService.create(file, id);
        userProfile.setAvatarUrl(imageMetadata.getId());

        return userProfileRepository.save(userProfile);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_ID, key = "#id"),
            @CacheEvict(value = CACHE_EMAIL, key = "#result.email", condition = "#result.email != null"),
            @CacheEvict(value = CACHE_LOGIN, key = "#result.login", condition = "#result.login != null")
    })
    public void updateEmail(String id, String email) {
        log.debug("Attempting update user email: {}", email);
        if (userProfileRepository.existsByEmail(email)) { // NOSONAR
            throw new AppException("Email already exists", 400);
        }
        UserProfile user = findById(id);
        user.setEmail(email);
        UserProfile saved = userProfileRepository.save(user);
        eventPublisher.publishEvent(UserUpdateEmailEvent.builder().user(saved).build());
        log.info("User profile updated: {}", saved);
    }

    @Override
    public void deleteById(String id) {
        log.debug("Attempting delete user profile: {}", id);
        userProfileRepository.deleteById(id);
        log.info("User profile deleted: {}", id);
    }

    private Supplier<AppException> notFound(String field, Object value) {
        return () -> {
            log.warn("UserProfile with {} {} not found", field, value);
            return new AppException(String.format("User with %s %s not found", field, value), 404);
        };
    }
}