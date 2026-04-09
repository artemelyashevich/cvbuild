package com.bsu.cvbuilder.annotation.otp;

import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OtpVerification {
    String key() default "deleteAccount";

    NotificationEngine engine() default NotificationEngine.EMAIL;

    String template() default "";
}