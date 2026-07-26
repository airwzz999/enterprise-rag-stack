package com.knowledge.base.common.annotation;

import java.lang.annotation.*;

/**
 * Rate limiting annotation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Rate limit key
     */
    String key() default "";

    /**
     * Time window (seconds)
     */
    int time() default 60;

    /**
     * Maximum number of requests allowed within the time window
     */
    int count() default 100;

    /**
     * Rate limit exceeded message
     */
    String message() default "Too many operations, please try again later";
}
