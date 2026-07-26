package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Operation log entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_operation_log")
@Schema(description = "Operation log entity")
public class OperationLog extends BaseEntity {

    @Schema(description = "Module name")
    @TableField("module")
    private String module;

    @Schema(description = "Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.")
    @TableField("operation_type")
    private String operationType;

    @Schema(description = "Operation description")
    @TableField("operation_desc")
    private String operationDesc;

    @Schema(description = "Request method: GET/POST/PUT/DELETE")
    @TableField("request_method")
    private String requestMethod;

    @Schema(description = "Request URL")
    @TableField("request_url")
    private String requestUrl;

    @Schema(description = "Request parameters (JSON)")
    @TableField("request_params")
    private String requestParams;

    @Schema(description = "Response result (JSON)")
    @TableField("response_result")
    private String responseResult;

    @Schema(description = "Operator user ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "Operator username")
    @TableField("username")
    private String username;

    @Schema(description = "IP address")
    @TableField("ip_address")
    private String ipAddress;

    @Schema(description = "Location")
    @TableField("location")
    private String location;

    @Schema(description = "User agent")
    @TableField("user_agent")
    private String userAgent;

    @Schema(description = "Execution time (ms)")
    @TableField("execute_time")
    private Integer executeTime;

    @Schema(description = "Status: 0-failed, 1-succeeded")
    @TableField("status")
    private Integer status;

    @Schema(description = "Error message")
    @TableField("error_msg")
    private String errorMsg;

}
