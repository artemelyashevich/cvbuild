package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.domain.dto.settings.UserSettings;

public interface SettingsService {

    void setPassword(PasswordDto passwordDto);

    void resetPassword(ResetPasswordDto resetPasswordDto);

    void agree();

    void deleteAccount(String otp);

    boolean enable2fa();

    UserSettings findSettings();

    void setVerification();
}
