package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.UserProfile;

public interface OtpService {

    String create(UserProfile userProfile, String key);

    boolean validateOtp(UserProfile userProfile, String otp, String key);

    boolean exists(String key);

    void invalidate(String s);
}
