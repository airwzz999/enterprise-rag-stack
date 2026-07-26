package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.ResultCode;

/**
 * Forbidden access exception class
 *
 * <p>Used to handle cases where a user does not have permission to access a resource</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public ForbiddenException() {
        super(ResultCode.FORBIDDEN);
    }

    /**
     * Constructor
     *
     * @param message the error message
     */
    public ForbiddenException(String message) {
        super(ResultCode.FORBIDDEN.getCode(), message);
    }

    /**
     * Constructor
     *
     * @param message the error message
     * @param cause   the cause
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
