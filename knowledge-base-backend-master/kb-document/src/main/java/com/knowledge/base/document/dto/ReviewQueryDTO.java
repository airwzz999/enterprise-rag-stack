package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Review query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Review query request")
public class ReviewQueryDTO {

    @Schema(description = "Current page")
    private Long current = 1L;

    @Schema(description = "Page size")
    private Long size = 10L;

    @Schema(description = "Review status: 0-pending, 1-approved, 2-rejected")
    private Integer status;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Author ID")
    private Long authorId;

    @Schema(description = "Keyword search")
    private String keyword;
}
