package com.community.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedissonLock {

    /** key 的前缀，默认取方法全限定名 */
    String prefixKey() default "";

    /** 锁的 key，支持 SpEL 表达式 */
    String key();

    /** 等待时间，默认 3s */
    long waitTime() default 3;

    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;
}
