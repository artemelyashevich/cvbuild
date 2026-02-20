package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;

import java.util.function.Consumer;

public interface SecureDataService {

    SecureData prepareData(UserProfile userProfile);

    boolean checkCredsAndIf2faIsRequire(UserProfile userProfile, AuthRequest authRequest);

    SecureData findByUserId(String id);

    void validateNewEvent(String userId, SecureEvent secureEvent);

    void loadSecureData(SecureData build);

    void update(String id, SecureEvent secureEvent, Consumer<SecureData> updater);

    void deleteByUserId(String id);
}
