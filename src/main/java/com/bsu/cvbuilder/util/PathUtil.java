package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtil {

    public static final String[] PUBLIC_RESOURCES = { // NOSONAR
            "/login/**",
            "/oauth2/**",
            "/favicon.ico",
            "/default-ui.css",
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    public static final String[] AUTH_RESOURCES = { // NOSONAR
            "/api/v1/auth/agree",
            "/api/v1/auth/reset-password"
    };

    public static final String LOGOUT_URL = "/api/v1/auth/logout";
}