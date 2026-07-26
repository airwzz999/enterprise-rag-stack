package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Operation log query DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used for
 * operation log query conditions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Operation log query parameters")
public class OperationLogQueryDTO extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Module name
     */
    @Schema(description = "Module name", example = "User Management")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type", example = "CREATE")
    private String operationType;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID", example = "1234567890123456789")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username", example = "admin")
    private String username;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status", example = "1")
    private Integer status;

    /**
     * Start time
     */
    @Schema(description = "Start time", example = "2024-01-01 00:00:00")
    private String startTime;

    /**
     * End time
     */
    @Schema(description = "End time", example = "2024-12-31 23:59:59")
    private String endTime;

    /**
     * Keyword search (operation description or request URL)
     */
    @Schema(description = "Keyword", example = "create")
    private String keyword;
}
