package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document review VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document review information")
public class DocumentReviewVO {

    @Schema(description = "Review record ID")
    private Long id;

    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Document title")
    private String documentTitle;

    @Schema(description = "Document author ID")
    private Long authorId;

    @Schema(description = "Document author name")
    private String authorName;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewer name")
    private String reviewerName;

    @Schema(description = "Review result: 1-approved, 2-rejected")
    private Integer reviewResult;

    @Schema(description = "Review comment")
    private String reviewComment;

    @Schema(description = "Status before review")
    private Integer beforeStatus;

    @Schema(description = "Review time")
    private LocalDateTime reviewedAt;

    @Schema(description = "Review round")
    private Integer reviewRound;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Document category ID")
    private Long categoryId;

    @Schema(description = "Document category name")
    private String categoryName;
}
