package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PathUtil {
    public static final String[] PUBLIC_RESOURCES = {
            "/login/**",
            "/oauth2/**",
            "/favicon.ico",
            "/default-ui.css",
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };
}