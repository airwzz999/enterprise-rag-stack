package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Operation log DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating an operation log</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Operation log request parameters")
public class OperationLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Log ID
     */
    @Schema(description = "Log ID", example = "1234567890123456789")
    private Long id;

    /**
     * Module name
     */
    @Schema(description = "Module name", required = true, example = "User Management")
    @NotBlank(message = "Module name must not be empty")
    @Size(max = 50, message = "Module name must not exceed 50 characters")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type", required = true, example = "CREATE")
    @NotBlank(message = "Operation type must not be empty")
    @Size(max = 20, message = "Operation type must not exceed 20 characters")
    private String operationType;

    /**
     * Operation description
     */
    @Schema(description = "Operation description", required = true, example = "Create user")
    @NotBlank(message = "Operation description must not be empty")
    @Size(max = 500, message = "Operation description must not exceed 500 characters")
    private String operationDesc;

    /**
     * Request method: GET/POST/PUT/DELETE
     */
    @Schema(description = "Request method", example = "POST")
    @Size(max = 10, message = "Request method must not exceed 10 characters")
    private String requestMethod;

    /**
     * Request URL
     */
    @Schema(description = "Request URL", example = "/api/users")
    @Size(max = 500, message = "Request URL must not exceed 500 characters")
    private String requestUrl;

    /**
     * Request parameters (JSON)
     */
    @Schema(description = "Request parameters", example = "{\"username\":\"zhangsan\"}")
    private String requestParams;

    /**
     * Response result (JSON)
     */
    @Schema(description = "Response result", example = "{\"code\":200,\"message\":\"success\"}")
    private String responseResult;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID", example = "1234567890123456789")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username", example = "admin")
    @Size(max = 50, message = "Operator username must not exceed 50 characters")
    private String username;

    /**
     * IP address
     */
    @Schema(description = "IP address", example = "192.168.1.1")
    @Size(max = 50, message = "IP address must not exceed 50 characters")
    private String ipAddress;

    /**
     * Location
     */
    @Schema(description = "Location", example = "Beijing")
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    /**
     * User agent
     */
    @Schema(description = "User agent", example = "Mozilla/5.0...")
    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    /**
     * Execution time (milliseconds)
     */
    @Schema(description = "Execution time (ms)", example = "100")
    private Integer executeTime;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;

    /**
     * Error message
     */
    @Schema(description = "Error message", example = "Operation failed")
    @Size(max = 2000, message = "Error message must not exceed 2000 characters")
    private String errorMsg;
}
