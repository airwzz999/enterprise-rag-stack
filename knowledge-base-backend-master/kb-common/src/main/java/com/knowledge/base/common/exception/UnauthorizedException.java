package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.ResultCode;

/**
 * Unauthorized exception class
 *
 * <p>Used to handle cases where a user is not logged in or the token is invalid</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class UnauthorizedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public UnauthorizedException() {
        super(ResultCode.UNAUTHORIZED);
    }

    /**
     * Constructor
     *
     * @param message the error message
     */
    public UnauthorizedException(String message) {
        super(ResultCode.UNAUTHORIZED.getCode(), message);
    }

    /**
     * Constructor
     *
     * @param message the error message
     * @param cause   the cause
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
