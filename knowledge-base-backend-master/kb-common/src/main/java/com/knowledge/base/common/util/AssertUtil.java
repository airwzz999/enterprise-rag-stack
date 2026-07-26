package com.knowledge.base.common.util;

import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.ResultCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Assertion utility class
 *
 * <p>Referenced susan-mall-cloud's AssertUtil implementation</p>
 * <p>Provides business parameter validation, throwing a BusinessException when validation fails</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class AssertUtil {

    /**
     * Assert that an expression is true
     *
     * @param expression the expression
     * @param message    error message
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an expression is false
     *
     * @param expression the expression
     * @param message    error message
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an object is null
     *
     * @param object  the object
     * @param message error message
     */
    public static void isNull(Object object, String message) {
        if (Objects.nonNull(object)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an object is not null
     *
     * @param object  the object
     * @param message error message
     */
    public static void notNull(Object object, String message) {
        if (Objects.isNull(object)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a string has length
     *
     * @param text    the string
     * @param message error message
     */
    public static void hasLength(String text, String message) {
        if (StringUtils.isEmpty(text)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a string has content (non-empty string)
     *
     * @param text    the string
     * @param message error message
     */
    public static void hasText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a collection is not empty
     *
     * @param collection the collection
     * @param message    error message
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a Map is not empty
     *
     * @param map     the Map
     * @param message error message
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an array has content
     *
     * @param array   the array
     * @param message error message
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert a state (general-purpose assertion)
     *
     * @param state   the state
     * @param message error message
     */
    public static void state(boolean state, String message) {
        if (!state) {
            throw new BusinessException(message);
        }
    }
}
