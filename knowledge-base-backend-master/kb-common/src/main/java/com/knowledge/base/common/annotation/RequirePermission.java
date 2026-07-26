package com.knowledge.base.common.annotation;

import java.lang.annotation.*;

/**
 * Permission validation annotation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * The required permission
     */
    String value();

    /**
     * Permission description
     */
    String description() default "";
}
