package com.bsu.cvbuilder.domain.dto.auth;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private boolean secondPhaseEnabled = false;

    @Builder.Default
    private String role = UserProfile.Role.USER.name();
}
