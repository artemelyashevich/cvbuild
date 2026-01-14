package com.bsu.cvbuilder.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomCorsConfiguration implements CorsConfigurationSource {

    private final ApplicationProperties applicationProperties;

    @Override
    public CorsConfiguration getCorsConfiguration(@NonNull HttpServletRequest request) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(applicationProperties.getSecurity().getAllowedOrigins()));
        config.setAllowedMethods(List.of(applicationProperties.getSecurity().getAllowedMethods()));
        config.setAllowedHeaders(List.of(applicationProperties.getSecurity().getAllowedHeaders()));
        config.setAllowCredentials(applicationProperties.getSecurity().getAllowCredentials());
        return config;
    }
}