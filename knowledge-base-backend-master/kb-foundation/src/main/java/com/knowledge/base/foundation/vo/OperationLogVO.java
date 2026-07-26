package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Operation log VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * operation log information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Operation log response")
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Log ID
     */
    @Schema(description = "Log ID")
    private Long id;

    /**
     * Module name
     */
    @Schema(description = "Module name")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type")
    private String operationType;

    /**
     * Operation description
     */
    @Schema(description = "Operation description")
    private String operationDesc;

    /**
     * Request method: GET/POST/PUT/DELETE
     */
    @Schema(description = "Request method")
    private String requestMethod;

    /**
     * Request URL
     */
    @Schema(description = "Request URL")
    private String requestUrl;

    /**
     * Request parameters (JSON)
     */
    @Schema(description = "Request parameters")
    private String requestParams;

    /**
     * Response result (JSON)
     */
    @Schema(description = "Response result")
    private String responseResult;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username")
    private String username;

    /**
     * IP address
     */
    @Schema(description = "IP address")
    private String ipAddress;

    /**
     * Location
     */
    @Schema(description = "Location")
    private String location;

    /**
     * User agent
     */
    @Schema(description = "User agent")
    private String userAgent;

    /**
     * Execution time (milliseconds)
     */
    @Schema(description = "Execution time (ms)")
    private Integer executeTime;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Error message
     */
    @Schema(description = "Error message")
    private String errorMsg;

    /**
     * Operation time
     */
    @Schema(description = "Operation time")
    private LocalDateTime createdAt;
}
