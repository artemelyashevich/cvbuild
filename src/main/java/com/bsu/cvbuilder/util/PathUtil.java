package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PathUtil {

    public static List<String> PUBLIC_RESOURCES = List.of(
            "/api/v1/auth/", "/login", "/oauth2/", "/favicon.ico",
            // FIXME!!!
            "/api/v1/ai-chat"
    );
    public static List<String> PRIVATE_RESOURCES = new ArrayList<>();
}
