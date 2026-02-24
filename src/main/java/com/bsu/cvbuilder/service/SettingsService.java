package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;

public interface SettingsService {

    void setPassword(PasswordDto passwordDto);

    void resetPassword(ResetPasswordDto resetPasswordDto);

    void agree();

    void deleteAccount();

    boolean enable2fa();
}
