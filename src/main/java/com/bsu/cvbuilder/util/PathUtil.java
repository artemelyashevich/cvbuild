package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class PathUtil {

    public static final String[] PUBLIC_RESOURCES = { // NOSONAR
            "/login/**",
            "/oauth2/**",
            "/favicon.ico",
            "/default-ui.css",
            "/api/v1/auth/**",
            "/api/v1/templates/**",
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

    public static final String[] ADMIN_RESOURCES = { // NOSONAR
            "/api/v1/stats/admin/**",
            //"/api/v1/templates/admin/**",
            "/api/v1/history/admin/**"
    };

    public static final String[] SUPER_ADMIN_RESOURCES = { // NOSONAR
            "api/v1/management/**"
    };

    public static final String LOGOUT_URL = "/api/v1/auth/logout";

    public static List<String> toList(String[] resources) {
        return Arrays.asList(resources);
    }
}