package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.image.ImageMetadata;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.domain.event.UserCreatedEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.ImageService;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageService imageService;
    private final MongoTemplate mongoTemplate;

    private static final String CACHE_ID = "user_id";
    private static final String CACHE_EMAIL = "user_email";
    private static final String CACHE_LOGIN = "user_login";

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
        return userProfileRepository.findByLogin(login)
                .orElseGet(() -> this.findByEmail(login));
    }

    @Override
    @Transactional(readOnly = true)
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
        log.debug("Creating user profile: {}", userProfile.getEmail());

        UserProfile saved = userProfileRepository.save(userProfile);
        eventPublisher.publishEvent(new UserCreatedEvent(saved));

        return saved;
    }

    @Override
    @Transactional
    public UserProfile login(String login) {
        log.debug("User login attempt: {}", login);

        UserProfile user = userProfileRepository.findByLogin(login)
                .orElseGet(() -> {
                    log.info("User not found, creating new profile for login: {}", login);
                    return userProfileRepository.save(UserProfile.builder()
                            .login(login)
                            .lastLogin(LocalDateTime.now())
                            .build()
                    );
                });

        user.setLastLogin(LocalDateTime.now());
        return userProfileRepository.save(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ID, key = "#profile.id"),
            @CacheEvict(value = CACHE_EMAIL, key = "#profile.email", condition = "#result.email != null"),
            @CacheEvict(value = CACHE_LOGIN, key = "#profile.login", condition = "#result.login != null")
    })
    public UserProfile update(UserProfile profile) {
        log.debug("Updating user profile: {}", profile.getId());

        UserProfile existingUser = userProfileRepository.findById(profile.getId())
                .orElseThrow(notFound("id", profile.getId()));

        existingUser.setAiLimits(profile.getAiLimits());
        existingUser.setLastLogin(profile.getLastLogin());
        existingUser.setFirstName(profile.getFirstName());
        existingUser.setLastName(profile.getLastName());

        return userProfileRepository.save(existingUser);
    }


    public UserProfile uploadAvatar(MultipartFile file, String id) {
        ImageMetadata imageMetadata = imageService.create(file, id);

        UserProfile userProfile = userProfileRepository.findById(id).orElseThrow(notFound("id", id));
        userProfile.setAvatarUrl(imageMetadata.getId());

        return userProfileRepository.save(userProfile);
    }

    private Supplier<AppException> notFound(String field, Object value) {
        return () -> {
            String msg = String.format("UserProfile with %s %s not found", field, value);
            log.warn(msg);
            return new AppException(msg, 404);
        };
    }
}