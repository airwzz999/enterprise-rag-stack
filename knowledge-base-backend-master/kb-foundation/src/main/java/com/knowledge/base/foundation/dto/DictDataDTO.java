package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Dictionary data DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating dictionary data</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Dictionary data request parameters")
public class DictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary data ID
     */
    @Schema(description = "Dictionary data ID", example = "1234567890123456789")
    private Long id;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID", required = true, example = "1234567890123456789")
    @NotNull(message = "Dictionary ID must not be empty")
    private Long dictId;

    /**
     * Dictionary code (redundant)
     */
    @Schema(description = "Dictionary code", example = "sys_user_gender")
    @Size(max = 100, message = "Dictionary code must not exceed 100 characters")
    private String dictCode;

    /**
     * Dictionary label
     */
    @Schema(description = "Dictionary label", required = true, example = "Male")
    @NotBlank(message = "Dictionary label must not be empty")
    @Size(max = 100, message = "Dictionary label must not exceed 100 characters")
    private String dictLabel;

    /**
     * Dictionary value
     */
    @Schema(description = "Dictionary value", required = true, example = "1")
    @NotBlank(message = "Dictionary value must not be empty")
    @Size(max = 100, message = "Dictionary value must not exceed 100 characters")
    private String dictValue;

    /**
     * Sort order
     */
    @Schema(description = "Sort order", example = "0")
    @NotNull(message = "Sort order must not be empty")
    private Integer dictSort;

    /**
     * CSS class name
     */
    @Schema(description = "CSS class name", example = "default")
    @Size(max = 100, message = "CSS class name must not exceed 100 characters")
    private String cssClass;

    /**
     * List style
     */
    @Schema(description = "List style", example = "primary")
    @Size(max = 100, message = "List style must not exceed 100 characters")
    private String listClass;

    /**
     * Is default: 0-no, 1-yes
     */
    @Schema(description = "Is default", example = "0")
    @NotNull(message = "Is default must not be empty")
    private Integer isDefault;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;
}
