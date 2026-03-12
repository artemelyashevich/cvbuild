package com.bsu.cvbuilder.domain.dto.settings;

import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
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

    boolean emailIsVerified;
    boolean isSecondAuthPhaseEnabled;
    boolean isPasswordSet;
    NotificationEngine notificationEngine;
}
