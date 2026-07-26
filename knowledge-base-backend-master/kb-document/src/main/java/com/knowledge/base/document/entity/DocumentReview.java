package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document review record entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("tb_document_review")
@Schema(description = "Document review record entity")
public class DocumentReview {

    /**
     * Review record ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Review record ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Reviewer ID
     */
    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    /**
     * Reviewer name
     */
    @Schema(description = "Reviewer name")
    private String reviewerName;

    /**
     * Review result: 1-approved, 2-rejected
     */
    @Schema(description = "Review result")
    private Integer reviewResult;

    /**
     * Review comment
     */
    @Schema(description = "Review comment")
    private String reviewComment;

    /**
     * Status before review
     */
    @Schema(description = "Status before review")
    private Integer beforeStatus;

    /**
     * Review time
     */
    @Schema(description = "Review time")
    private LocalDateTime reviewedAt;

    /**
     * Review round
     */
    @Schema(description = "Review round")
    private Integer reviewRound;

    /**
     * Review level (1=first-level review, reserved for multi-level expansion)
     */
    @Schema(description = "Review level")
    private Integer reviewLevel;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
