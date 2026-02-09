package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;

public interface SecureDataService {

    SecureData prepareData(UserProfile userProfile);

    void checkData(UserProfile userProfile, AuthRequest authRequest);

    SecureData findByUserId(String id);

    void loadSecureData(SecureData build);
}
