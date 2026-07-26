package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User favorite VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "User favorite information")
public class UserFavoriteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Favorite ID
     */
    @Schema(description = "Favorite ID")
    private Long id;

    /**
     * User ID
     */
    @Schema(description = "User ID")
    private Long userId;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String documentTitle;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    private String documentSummary;

    /**
     * Document category ID
     */
    @Schema(description = "Document category ID")
    private Long documentCategoryId;

    /**
     * Document category name
     */
    @Schema(description = "Document category name")
    private String documentCategoryName;

    /**
     * Document author ID
     */
    @Schema(description = "Document author ID")
    private Long documentAuthorId;

    /**
     * Document author name
     */
    @Schema(description = "Document author name")
    private String documentAuthorName;

    /**
     * Document status
     */
    @Schema(description = "Document status")
    private Integer documentStatus;

    /**
     * View count
     */
    @Schema(description = "View count")
    private Long documentViewCount;

    /**
     * Favorite time
     */
    @Schema(description = "Favorite time")
    private LocalDateTime favoriteTime;

    /**
     * Whether favorited
     */
    @Schema(description = "Whether favorited")
    private Boolean isFavorited;
}
