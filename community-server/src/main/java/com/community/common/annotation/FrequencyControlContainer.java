package com.community.common.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FrequencyControlContainer {
    FrequencyControl[] value();
}
