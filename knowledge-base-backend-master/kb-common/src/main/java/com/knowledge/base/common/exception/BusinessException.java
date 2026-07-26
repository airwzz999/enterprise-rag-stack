package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.ResultCode;
import lombok.Getter;

/**
 * Business exception class
 *
 * <p>Used to handle exceptional conditions in business logic, designed following the Alibaba Java Development Guidelines</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code
     */
    private final Integer code;

    /**
     * Error message
     */
    private final String message;

    /**
     * Constructor
     *
     * @param message the error message
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param code    the error code
     * @param message the error message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param resultCode the result code enum
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * Constructor
     *
     * @param message the error message
     * @param cause   the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param resultCode the result code enum
     * @param cause      the cause
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}
