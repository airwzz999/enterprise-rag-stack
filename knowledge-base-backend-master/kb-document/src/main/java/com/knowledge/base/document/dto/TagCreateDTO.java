package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Tag creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Tag creation request")
public class TagCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    @NotBlank(message = "Tag name must not be blank")
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
     * Tag type: 0-SYSTEM, 1-USER
     */
    @Schema(description = "Tag type")
    private Integer tagType;

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
}
