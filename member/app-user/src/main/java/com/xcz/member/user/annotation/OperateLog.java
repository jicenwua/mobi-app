package com.xcz.member.user.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     */
    String operation() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否保存返回结果
     */
    boolean saveResult() default false;
}
