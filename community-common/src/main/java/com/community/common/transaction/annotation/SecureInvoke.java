package com.community.common.transaction.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 保证方法成功执行。事务内的方法会将操作记录入库，事务提交后执行，失败自动重试。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SecureInvoke {

    /**
     * 最大重试次数（包括第一次正常执行），默认 3 次
     */
    int maxRetryTimes() default 3;

    /**
     * 是否异步执行。默认 true（事务提交后异步执行，不阻塞主线程）。
     * 同步执行适合 MQ 消费场景等对耗时不关心、但希望链路追踪不被异步影响的场景。
     */
    boolean async() default true;
}
