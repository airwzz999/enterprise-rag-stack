package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Dictionary DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating a dictionary type</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Dictionary type request parameters")
public class DictDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID", example = "1234567890123456789")
    private Long id;

    /**
     * Dictionary code
     */
    @Schema(description = "Dictionary code", required = true, example = "sys_user_gender")
    @NotBlank(message = "Dictionary code must not be empty")
    @Size(max = 100, message = "Dictionary code must not exceed 100 characters")
    private String dictCode;

    /**
     * Dictionary name
     */
    @Schema(description = "Dictionary name", required = true, example = "User gender")
    @NotBlank(message = "Dictionary name must not be empty")
    @Size(max = 100, message = "Dictionary name must not exceed 100 characters")
    private String dictName;

    /**
     * Dictionary type
     */
    @Schema(description = "Dictionary type", required = true, example = "system")
    @NotBlank(message = "Dictionary type must not be empty")
    @Size(max = 50, message = "Dictionary type must not exceed 50 characters")
    private String dictType;

    /**
     * Description
     */
    @Schema(description = "Description", example = "User gender dictionary")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Sort order
     */
    @Schema(description = "Sort order", example = "0")
    @NotNull(message = "Sort order must not be empty")
    private Integer sort;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;
}
