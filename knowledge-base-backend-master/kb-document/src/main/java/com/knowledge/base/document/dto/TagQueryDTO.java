package com.knowledge.base.document.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tag query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Tag query request")
public class TagQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    /**
     * Tag name (fuzzy query)
     */
    @Schema(description = "Tag name")
    private String tagName;

    /**
     * Tag type: 0-SYSTEM, 1-USER
     */
    @Schema(description = "Tag type")
    private Integer tagType;

    /**
     * Parent category ID
     */
    @Schema(description = "Parent category ID")
    private Long categoryId;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
