package com.bsu.cvbuilder.domain.dto.settings;

import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSettings {

    private boolean emailIsVerified;
    private boolean isSecondAuthPhaseEnabled;
    private boolean isPasswordSet;
    private NotificationEngine notificationEngine;
    private boolean isAgree;
}
