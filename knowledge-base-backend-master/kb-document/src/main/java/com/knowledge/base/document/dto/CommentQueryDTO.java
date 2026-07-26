package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Comment query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Comment query request")
public class CommentQueryDTO {

    @Schema(description = "Current page")
    private Long current = 1L;

    @Schema(description = "Page size")
    private Long size = 10L;

    @Schema(description = "Sort field: like_count-like count, created_at-creation time")
    private String sortBy = "created_at";

    @Schema(description = "Sort direction: asc-ascending, desc-descending")
    private String sortOrder = "desc";
}
