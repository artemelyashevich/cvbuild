package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PathUtil {

    public static List<String> PUBLIC_RESOURCES = List.of(
            "/login", "/oauth2/", "/favicon.ico", "/login/**", "/favicon.ico", "/default-ui.css",
            "/api/v1/auth/login", "/api/v1/auth/register", "/swagger-ui/**"
    );
}
