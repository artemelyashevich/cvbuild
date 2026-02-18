package com.bsu.cvbuilder.domain.dto.auth;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@Data
@RequestScope
public class SecurityProvider {
    private UserProfile userProfile;
    private String token;
    private Authentication authentication;
}