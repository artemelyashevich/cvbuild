package com.bsu.cvbuilder.annotation.otp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OtpRequire {
    boolean value() default true;

    String key() default "otp:email:";
}
