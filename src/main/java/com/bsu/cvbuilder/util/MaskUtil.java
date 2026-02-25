package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MaskUtil {
    public static String maskFirstFive(String input) {
        if (input == null || input.isEmpty()) return input;
        int visible = Math.min(5, input.length());
        return input.substring(0, visible) + "*".repeat(input.length() - visible);
    }

    public static String maskFraction(String input) {
        if (input == null || input.isEmpty()) return input;
        int visible = Math.max(1, (int) Math.floor(input.length() * 0.4));
        return input.substring(0, visible) + "*".repeat(input.length() - visible);
    }

    public static String mask(String input, int visibleChars) {
        if (input == null || input.isEmpty()) return input;
        int visible = Math.min(Math.max(0, visibleChars), input.length());
        return input.substring(0, visible) + "*".repeat(input.length() - visible);
    }
}
