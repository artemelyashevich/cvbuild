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
            "/error",
            "/actuator/**",
            "/test/**"
    };

    public static final String[] AUTH_RESOURCES = { // NOSONAR
            "/api/v1/auth/agree",
            "/api/v1/auth/reset-password"
    };

    public static final String[] ADMIN_RESOURCES = {
            "/api/v1/stats/admin/**",
            "/api/v1/templates/admin/**",
            "/api/v1/history/admin/**"
    };

    public static final String LOGOUT_URL = "/api/v1/auth/logout";
}