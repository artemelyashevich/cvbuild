package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ApplicationContext applicationContext;

    @Override
    @Caching(
            put = {
                    @CachePut(value = "user::email", key = "#email"),
                    @CachePut(value = "user::login", key = "#result.login"),
                    @CachePut(value = "user::id", key = "#result.id")
            }
    )
    public UserProfile findByEmail(String email) {
        log.debug("Attempting to find user profile by email {}", email);
        var user = userProfileRepository.findByEmail(email).orElseThrow(() -> {
            var message = String.format("UserProfile with email %s not found", email);
            log.error(message);
            return new AppException(message, 404);
        });
        log.info("UserProfile found by email {}", user);
        return user;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "user::login", key = "#result.login"),
                    @CachePut(value = "user::email", key = "#result.email"),
                    @CachePut(value = "user::id", key = "#result.id")
            }
    )
    public UserProfile findByLogin(String login) {
        log.debug("Attempting to find user profile by login {}", login);
        var user = userProfileRepository.findByLogin(login).orElseThrow(() -> {
            var message = String.format("UserProfile with login %s not found", login);
            log.error(message);
            return new AppException(message, 404);
        });
        log.info("UserProfile found by login {}", user);
        return user;
    }


    @Override
    @CachePut(value = "user::id", key = "#id")
    public UserProfile findById(String id) {
        log.debug("Attempting to find user profile by id {}", id);
        var user = userProfileRepository.findById(id).orElseThrow(() -> {
            var message = String.format("UserProfile with id %s not found", id);
            log.error(message);
            return new AppException(message, 404);
        });
        log.info("UserProfile found by id {}", user);
        return user;
    }

    @Override
    public Boolean existsByEmail(String email) {
        log.debug("Attempting checking if user profile exists by email {}", email);
        return userProfileRepository.existsByEmail(email);
    }

    @Override
    public UserProfile create(UserProfile build) {
        log.debug("Attempting to create user profile {}", build);

        var newUserProfile = userProfileRepository.save(build);

        log.info("UserProfile created {}", newUserProfile);
        return newUserProfile;
    }

    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public synchronized UserProfile login(String login) {
        log.debug("Attempting to login user profile by email {}", login);
        UserProfile user;
        try {
            user = applicationContext.getBean(UserProfileServiceImpl.class).findByLogin(login);
            user.setLastLogin(LocalDateTime.now());
            user = userProfileRepository.save(user);
        } catch (AppException ignored) {
            user = applicationContext.getBean(UserProfileServiceImpl.class).create(UserProfile.builder()
                    .login(login)
                    .lastLogin(LocalDateTime.now())
                    .build());
        }
        log.info("UserProfile logged in");
        return user;
    }

    @Override
    public UserProfile update(UserProfile profile) {
        log.debug("Attempting to update user profile {}", profile);
        var user = userProfileRepository.findByEmail(profile.getEmail()).orElseThrow(
                () -> {
                    var message = String.format("UserProfile with email %s not found", profile.getEmail());
                    log.error(message);
                    return new AppException(message, 404);
                }
        );

        user.setAiLimits(profile.getAiLimits());

        var updatedUser = userProfileRepository.save(user);

        log.info("UserProfile updated {}", updatedUser);
        return updatedUser;
    }
}
