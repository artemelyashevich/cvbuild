package com.bsu.cvbuilder.domain.dto.auth;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Data
public class SecurityProvider {
    private String token;
}