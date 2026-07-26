package com.knowledge.base.common.annotation;

import java.lang.annotation.*;

/**
 * Operation log annotation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * Operation module
     */
    String module() default "";

    /**
     * Operation type
     */
    String operation() default "";

    /**
     * Operation description
     */
    String description() default "";
}
