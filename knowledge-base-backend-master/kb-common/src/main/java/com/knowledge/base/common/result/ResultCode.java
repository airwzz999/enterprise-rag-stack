package com.knowledge.base.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response code enum
 *
 * <p>Designed following the Alibaba Java Development Guidelines, uniformly manages system response codes</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * Success
     */
    SUCCESS(200, "Operation succeeded"),

    /**
     * Failure
     */
    ERROR(500, "Operation failed"),

    /**
     * Parameter error
     */
    PARAM_ERROR(400, "Parameter error"),

    /**
     * Missing parameter
     */
    PARAM_MISSING(400, "Missing parameter"),

    /**
     * Invalid parameter
     */
    PARAM_INVALID(400, "Invalid parameter"),

    /**
     * Unauthorized
     */
    UNAUTHORIZED(401, "Unauthorized, please log in first"),

    /**
     * Access forbidden
     */
    FORBIDDEN(403, "Access forbidden"),

    /**
     * Resource not found
     */
    NOT_FOUND(404, "Resource not found"),

    /**
     * Request method not allowed
     */
    METHOD_NOT_ALLOWED(405, "Request method not allowed"),

    /**
     * Request timeout
     */
    REQUEST_TIMEOUT(408, "Request timeout"),

    /**
     * Internal server error
     */
    INTERNAL_SERVER_ERROR(500, "Internal server error"),

    /**
     * Service unavailable
     */
    SERVICE_UNAVAILABLE(503, "Service unavailable"),

    /**
     * Incorrect username or password
     */
    USERNAME_OR_PASSWORD_ERROR(10001, "Incorrect username or password"),

    /**
     * User does not exist
     */
    USER_NOT_EXIST(10002, "User does not exist"),

    /**
     * User already exists
     */
    USER_ALREADY_EXIST(10003, "User already exists"),

    /**
     * User has been disabled
     */
    USER_DISABLED(10004, "User has not been activated"),

    /**
     * Invalid token
     */
    TOKEN_INVALID(10005, "Invalid token"),

    /**
     * Token has expired
     */
    TOKEN_EXPIRED(10006, "Token has expired"),

    /**
     * Access denied
     */
    ACCESS_DENIED(10007, "Access denied"),

    /**
     * Document does not exist
     */
    DOCUMENT_NOT_EXIST(20001, "Document does not exist"),

    /**
     * Document already exists
     */
    DOCUMENT_ALREADY_EXIST(20002, "Document already exists"),

    /**
     * Document category does not exist
     */
    CATEGORY_NOT_EXIST(20003, "Document category does not exist"),

    /**
     * File upload failed
     */
    FILE_UPLOAD_FAILED(20004, "File upload failed"),

    /**
     * Unsupported file type
     */
    FILE_TYPE_NOT_SUPPORTED(20005, "Unsupported file type"),

    /**
     * File size exceeds the limit
     */
    FILE_SIZE_EXCEEDED(20006, "File size exceeds the limit");

    /**
     * Response code
     */
    private final Integer code;

    /**
     * Response message
     */
    private final String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
