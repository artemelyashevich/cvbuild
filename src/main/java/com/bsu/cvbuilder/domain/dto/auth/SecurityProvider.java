package com.bsu.cvbuilder.domain.dto.auth;

import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Data
public class SecurityProvider {
    private String token;
    private Authentication authentication;
}