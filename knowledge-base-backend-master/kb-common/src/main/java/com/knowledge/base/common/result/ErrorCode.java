package com.knowledge.base.common.result;

import lombok.Getter;

/**
 * Error code enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public enum ErrorCode {

    /**
     * General error codes
     */
    SUCCESS(200, "Operation succeeded"),
    ERROR(500, "System error"),
    BAD_REQUEST(400, "Invalid request parameters"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Access forbidden"),
    NOT_FOUND(404, "Resource not found"),

    /**
     * User-related error codes (1000-1999)
     */
    USER_NOT_FOUND(1001, "User not found"),
    USER_PASSWORD_ERROR(1002, "Incorrect password"),
    USER_ACCOUNT_DISABLED(1003, "Account has been disabled"),
    USER_ALREADY_EXISTS(1004, "User already exists"),
    USER_TOKEN_INVALID(1005, "Invalid token"),
    USER_TOKEN_EXPIRED(1006, "Token has expired"),

    /**
     * Document-related error codes (2000-2999)
     */
    DOC_NOT_FOUND(2001, "Document not found"),
    DOC_ALREADY_EXISTS(2002, "Document already exists"),
    DOC_VERSION_ERROR(2003, "Document version error"),
    DOC_STATUS_ERROR(2004, "Document status error"),
    DOC_PERMISSION_DENIED(2005, "No permission to operate on this document"),

    /**
     * File-related error codes (3000-3999)
     */
    FILE_NOT_FOUND(3001, "File not found"),
    FILE_UPLOAD_ERROR(3002, "File upload failed"),
    FILE_SIZE_EXCEED(3003, "File size exceeds the limit"),
    FILE_TYPE_ERROR(3004, "Unsupported file type"),
    FILE_DOWNLOAD_ERROR(3005, "File download failed"),

    /**
     * Permission-related error codes (4000-4999)
     */
    PERMISSION_NOT_FOUND(4001, "Permission not found"),
    PERMISSION_DENIED(4002, "Insufficient permissions"),
    ROLE_NOT_FOUND(4003, "Role not found"),
    ROLE_ALREADY_EXISTS(4004, "Role already exists"),

    /**
     * Team-related error codes (5000-5999)
     */
    TEAM_NOT_FOUND(5001, "Team not found"),
    TEAM_ALREADY_EXISTS(5002, "Team already exists"),
    TEAM_MEMBER_EXISTS(5003, "Member already exists"),
    TEAM_MEMBER_NOT_FOUND(5004, "Member not found"),

    /**
     * Search-related error codes (6000-6999)
     */
    SEARCH_ERROR(6001, "Search failed"),
    SEARCH_INDEX_ERROR(6002, "Index creation failed"),

    /**
     * Business-related error codes (7000-7999)
     */
    BUSINESS_ERROR(7001, "Business processing failed"),
    DATA_CONFLICT(7002, "Data conflict"),
    OPERATION_TOO_FREQUENT(7003, "Operation too frequent");

    /**
     * Error code
     */
    private final Integer code;

    /**
     * Error message
     */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
