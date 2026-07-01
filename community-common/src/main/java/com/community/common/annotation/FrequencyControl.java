package com.community.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Repeatable(FrequencyControlContainer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FrequencyControl {

    /** key 前缀，默认取方法全限定名 */
    String prefixKey() default "";

    /** 限流目标类型 */
    Target target() default Target.EL;

    /** SpEL 表达式（target=EL 时使用） */
    String spEl() default "";

    /** 单位时间内最大访问次数 */
    int count();

    /** 频控时间 */
    int time();

    /** 频控时间单位，默认秒 */
    TimeUnit unit() default TimeUnit.SECONDS;

    enum Target {
        UID, EL, IP
    }
}
