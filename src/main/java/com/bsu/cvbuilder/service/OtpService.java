package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;

public interface OtpService {

    String create(UserProfile userProfile);

    boolean validateOtp(UserProfile userProfile, String otp);
}
