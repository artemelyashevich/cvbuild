package com.bsu.cvbuilder.annotation;

public @interface Limited {
    LimitType value();
    int capacity() default 20;
}
