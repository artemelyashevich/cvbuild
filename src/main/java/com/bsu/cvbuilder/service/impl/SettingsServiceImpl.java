package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.domain.dto.settings.UserSettings;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AgreementEvent;
import com.bsu.cvbuilder.domain.event.ResetPasswordEvent;
import com.bsu.cvbuilder.domain.event.SetPasswordEvent;
import com.bsu.cvbuilder.domain.event.UserDeletedEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.HistoryService;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.SettingsService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.util.LockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final SecurityService securityService;
    private final SecureDataService secureDataService;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileService userProfileService;
    private final ChatService chatService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final HistoryService historyService;
    private final LockService lockService;
    private final AuthService authService;

    @Override
    @Transactional
    public void setPassword(PasswordDto passwordDto) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.debug("Attempting set new password for user {}", userProfile.getLogin());

        if (!passwordDto.confirmedPassword().equals(passwordDto.newPassword())) {
            log.debug("Passwords don't match: {}", userProfile.getLogin());
            throw new AppException("Passwords don't match", 401);
        }

        SecureData secureData = secureDataService.findByUserId(userProfile.getId());
        secureDataService.validateNewEvent(userProfile.getId(), SecureEvent.setPassword);
        if (secureData.getPassword() != null) {
            log.debug("Password already set for user {}", userProfile.getLogin());
            throw new AppException("Password already set", 401);
        }

        Map<SecureEvent, List<LocalDateTime>> secureEvents = secureData.getSecureEvents();
        if (secureEvents.containsKey(SecureEvent.resetPassword)) {
            log.debug("Password already set for user {}", userProfile.getLogin());
            throw new AppException("Password already set", 401);
        }

        secureDataService.update(userProfile.getId(), data -> {
            data.setPassword(passwordEncoder.encode(passwordDto.newPassword()));
            data.addEvent(SecureEvent.setPassword);
        });

        log.info("Password updated for user {}", userProfile.getLogin());
        applicationEventPublisher.publishEvent(new SetPasswordEvent(userProfile.getId()));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        log.debug("Attempting reset password");

        if (!resetPasswordDto.newPassword().equals(resetPasswordDto.confirmedNewPassword())) {
            log.debug("Passwords do not match");
            throw new AppException("Password do not match", 400);
        }

        UserProfile currentUser = securityService.findCurrentUser();

        SecureData secureData = secureDataService.findByUserId(currentUser.getId());

        if (!passwordEncoder.matches(resetPasswordDto.oldPassword(), secureData.getPassword())) {
            throw new AppException("Old password do not match", 401);
        }

        secureDataService.validateNewEvent(currentUser.getId(), SecureEvent.resetPassword);

        secureDataService.update(currentUser.getId(), data -> {
            data.addEvent(SecureEvent.resetPassword);
            data.setPassword(passwordEncoder.encode(resetPasswordDto.newPassword()));
        });

        log.info("Password has been reset for user: {}", currentUser.getLogin());
        applicationEventPublisher.publishEvent(new ResetPasswordEvent(currentUser.getId()));
    }

    @Override
    @Transactional
    public boolean agree() {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting process user agreement: {}", user.getLogin());
        secureDataService.validateNewEvent(user.getId(), SecureEvent.agreement);
        AtomicBoolean agree = new AtomicBoolean(true);
        secureDataService.update(user.getId(), data -> {
            data.addEvent(SecureEvent.agreement);
            agree.set(!data.isAgree());
            data.setAgree(agree.get());
        });
        applicationEventPublisher.publishEvent(new AgreementEvent(user.getId()));
        log.info("User has been agreed for user: {}, is agree: {}", user.getLogin(), agree.get());
        return agree.get();
    }

    @Override
    public void deleteAccount(String otp) {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting to delete account: {}", user.getLogin());
        boolean success = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            secureDataService.validateNewEvent(user.getId(), SecureEvent.deleteAccount);
            try {
                performUserDeletion(user);
                authService.logout();
            } catch (AppException e) {
                log.error("Failed to delete account for user {}: {}", user.getLogin(), e.getMessage(), e);
                secureDataService.update(user.getId(), data -> data.addEvent(SecureEvent.deleteAccount));
                status.setRollbackOnly();
            }
            return true;
        }));

        if (success) {
            applicationEventPublisher.publishEvent(new UserDeletedEvent(user.getId()));
            log.info("Account has been deleted: {}", user.getLogin());
        }
    }

    private void performUserDeletion(UserProfile user) throws AppException {
        lockService.withLock(LockUtil.USER.formatted(user.getId()), () -> {
            chatService.deleteAllByUserId(user.getId());
            secureDataService.deleteByUserId(user.getId());
            historyService.deleteAllByUserId(user.getId());
            userProfileService.deleteById(user.getId());
            return null;
        });
    }

    @Override
    @Transactional
    public boolean enable2fa() {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting to enable 2FA for user: {}", user.getLogin());
        secureDataService.validateNewEvent(user.getId(), SecureEvent.enable2fa);
        AtomicBoolean isEnabled = new AtomicBoolean(false);
        secureDataService.update(user.getId(), data -> {
            isEnabled.set(!data.getSecondAuthPhaseRequire());
            data.setSecondAuthPhaseRequire(!data.getSecondAuthPhaseRequire());
            data.addEvent(SecureEvent.enable2fa);
            log.info("2FA has been enabled for user: {} / {}", user.getLogin(), data.getSecondAuthPhaseRequire());
        });
        return isEnabled.get();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSettings findSettings() {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting find settings for user with login: {}", user.getLogin());
        UserSettings userSettings = new UserSettings();
        SecureData secureData = secureDataService.findByUserId(user.getId());
        userSettings.setPasswordSet(secureData.getPassword() != null);
        userSettings.setSecondAuthPhaseEnabled(secureData.getSecondAuthPhaseRequire());
        userSettings.setEmailIsVerified(secureData.getEmailVerified());
        userSettings.setNotificationEngine(secureData.getPreferableNotificationEngine());
        userSettings.setAgree(secureData.isAgree());
        return userSettings;
    }

    @Override
    public void deactivateLimits(String userId) {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting to deactivate limits for user: {}", user.getLogin());
        secureDataService.update(user.getId(), SecureData::clearEvents);
        log.info("Limits have been deactivated for user: {}", user.getLogin());
    }
}
