package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tag VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tag information")
public class TagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag ID
     */
    @Schema(description = "Tag ID")
    private Long id;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    private String tagName;

    /**
     * Tag code
     */
    @Schema(description = "Tag code")
    private String tagCode;

    /**
     * Parent category ID
     */
    @Schema(description = "Parent category ID")
    private Long categoryId;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String categoryName;

    /**
     * Tag type: 0-SYSTEM, 1-USER
     */
    @Schema(description = "Tag type")
    private Integer tagType;

    /**
     * Color
     */
    @Schema(description = "Color")
    private String color;

    /**
     * Icon
     */
    @Schema(description = "Icon")
    private String icon;

    /**
     * Document count
     */
    @Schema(description = "Document count")
    private Integer docCount;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
