package com.bsu.cvbuilder.service.integration;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.impl.UserProfileServiceImpl;
import com.bsu.cvbuilder.service.provider.UserProfileTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RecordApplicationEvents
class UserProfileServiceImplIntegrationTest extends AbstractTest {

    @Autowired
    private UserProfileServiceImpl userProfileService;

    @MockitoSpyBean
    private UserProfileRepository userProfileRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setup() {
        // Clean database and cache
        mongoTemplate.dropCollection(UserProfile.class);
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());


        doAnswer(invocation -> invocation.getArgument(0))
                .when(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("findById_ConsecutiveCalls_HitsCacheAfterFirstCall")
    void findById_ConsecutiveCalls_HitsCacheAfterFirstCall() {
        // Arrange
        var profile = UserProfileTestData.createNewProfile("cache@test.com", "cacheuser");
        var saved = mongoTemplate.save(profile);
        var id = saved.getId();

        // Act
        userProfileService.findById(id); // Call 1: DB Hit
        userProfileService.findById(id); // Call 2: Cache Hit

        // Assert
        verify(userProfileRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("login_ExistingUser_UpdatesLastLogin")
    void login_ExistingUser_UpdatesLastLogin() {
        // Arrange
        var login = "existing_user";
        var existing = mongoTemplate.save(UserProfile.builder().login(login).build());
        var initialLoginTime = existing.getLastLogin();

        // Act
        var result = userProfileService.login(login);

        // Assert
        assertAll(
                () -> assertEquals(existing.getId(), result.getId()),
                () -> assertNotEquals(initialLoginTime, result.getLastLogin()),
                () -> verify(userProfileRepository, atLeastOnce()).findByLogin(login)
        );
    }
}