package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document review DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document review parameters")
public class DocumentReviewDTO {

    /**
     * Review record ID
     */
    @Schema(description = "Review record ID")
    @NotNull(message = "Review record ID must not be null")
    private Long reviewId;

    /**
     * Review result: 1-approved, 2-rejected
     */
    @Schema(description = "Review result: 1-approved, 2-rejected")
    @NotNull(message = "Review result must not be null")
    private Integer reviewResult;

    /**
     * Review comment
     */
    @Schema(description = "Review comment")
    private String reviewComment;
}
