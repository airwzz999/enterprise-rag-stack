package com.knowledge.base.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Operation log event DTO
 *
 * <p>Carries operation log data over RabbitMQ; consumed by kb-foundation and written to the kb_operation_log table</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Module name */
    private String module;

    /** Operation type */
    private String operationType;

    /** Operation description */
    private String operationDesc;

    /** Request method */
    private String requestMethod;

    /** Request URL */
    private String requestUrl;

    /** Request parameters (JSON) */
    private String requestParams;

    /** User ID */
    private Long userId;

    /** Username */
    private String username;

    /** IP address */
    private String ipAddress;

    /** User agent */
    private String userAgent;

    /** Execution duration (milliseconds) */
    private Integer executeTime;

    /** Status: 1 - success, 0 - failure */
    private Integer status;

    /** Error message */
    private String errorMsg;
}
