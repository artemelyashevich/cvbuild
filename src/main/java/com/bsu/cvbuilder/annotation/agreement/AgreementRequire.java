package com.bsu.cvbuilder.annotation.agreement;

import com.bsu.cvbuilder.annotation.email.EmailVerification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@EmailVerification
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgreementRequire {
}
