package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Tag update DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Tag update request")
public class TagUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag ID
     */
    @Schema(description = "Tag ID")
    @NotNull(message = "Tag ID must not be null")
    private Long id;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String tagName;

    /**
     * Tag code
     */
    @Schema(description = "Tag code")
    @Size(max = 50, message = "Tag code must not exceed 50 characters")
    private String tagCode;

    /**
     * Parent category ID
     */
    @Schema(description = "Parent category ID")
    private Long categoryId;

    /**
     * Color
     */
    @Schema(description = "Color")
    @Size(max = 20, message = "Color value must not exceed 20 characters")
    private String color;

    /**
     * Icon
     */
    @Schema(description = "Icon")
    @Size(max = 50, message = "Icon value must not exceed 50 characters")
    private String icon;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
