package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheUtil {

    public static final String EMAIL_KEY = "otp:email";
    public static final String SECOND_AUTH_PHASE_KEY = "otp:email";
    public static final String ATTEMPTS_KEY = "otp:attempts:";
    public static final String BLOCKED_KEY = "otp:blocked:";
    public static final String NOTIFICATION_PROCESSING = "notification:dlq:";
    public static final String NOTIFICATION_DELAYED_KEY = "notification:delay:";
    public static final String NOTIFICATION_RETRY_KEY = "notification:retry:";
}
