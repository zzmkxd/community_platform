package com.community.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 频控注解
 */
@Repeatable(FrequencyControlContainer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FrequencyControl {

    /** 时间窗口数量 */
    int windowSize() default 5;

    /** 时间窗口 */
    int period();

    /** 时间单位，默认秒 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /** 限流 key，支持 SpEL 表达式 */
    String key();
}
