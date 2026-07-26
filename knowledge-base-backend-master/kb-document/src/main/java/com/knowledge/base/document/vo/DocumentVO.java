package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document response VO
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, used to return document information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document information response")
public class DocumentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long id;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    private String summary;

    /**
     * Document content
     */
    @Schema(description = "Document content")
    private String content;

    /**
     * Content length (character count)
     */
    @Schema(description = "Content length (character count)")
    private Integer contentLength;

    /**
     * Document type (1-article, 2-file)
     */
    @Schema(description = "Document type")
    private Integer documentType;

    /**
     * File path
     */
    @Schema(description = "File path")
    private String filePath;

    /**
     * File size (bytes)
     */
    @Schema(description = "File size")
    private Long fileSize;

    /**
     * File extension
     */
    @Schema(description = "File extension")
    private String fileExtension;

    /**
     * Category ID
     */
    @Schema(description = "Category ID")
    private Long categoryId;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String categoryName;

    /**
     * Team space ID
     */
    @Schema(description = "Team space ID")
    private Long teamId;

    /**
     * Team space name
     */
    @Schema(description = "Team space name")
    private String teamName;

    /**
     * Tag list
     */
    @Schema(description = "Tag list")
    private String tags;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Whether public
     */
    @Schema(description = "Whether public")
    private Integer isPublic;

    /**
     * Whether pinned to top
     */
    @Schema(description = "Whether pinned to top")
    private Integer isTop;

    /**
     * Whether recommended
     */
    @Schema(description = "Whether recommended")
    private Integer isRecommend;

    /**
     * View count
     */
    @Schema(description = "View count")
    private Long viewCount;

    /**
     * Like count
     */
    @Schema(description = "Like count")
    private Long likeCount;

    /**
     * Whether the current user has liked it
     */
    @Schema(description = "Whether the current user has liked it")
    private Boolean isLiked;

    /**
     * Favorite count
     */
    @Schema(description = "Favorite count")
    private Long favoriteCount;

    /**
     * Comment count
     */
    @Schema(description = "Comment count")
    private Long commentCount;

    /**
     * Publish time
     */
    @Schema(description = "Publish time")
    private LocalDateTime publishTime;

    /**
     * Author ID
     */
    @Schema(description = "Author ID")
    @Deprecated
    private Long authorId;

    /**
     * Author name
     */
    @Schema(description = "Author name")
    @Deprecated
    private String authorName;

    /**
     * Author information
     */
    @Schema(description = "Author information")
    private AuthorVO author;

    /**
     * Cover image URL
     */
    @Schema(description = "Cover image URL")
    private String coverImage;

    /**
     * Source
     */
    @Schema(description = "Source")
    private Integer source;

    /**
     * Source URL
     */
    @Schema(description = "Source URL")
    private String sourceUrl;

    /**
     * Allow comments
     */
    @Schema(description = "Allow comments")
    private Integer allowComment;

    /**
     * Whether the auto-save hint has been dismissed
     * <p>null = not created via auto-save, 0 = created via auto-save and hint not dismissed, 1 = hint dismissed</p>
     */
    @Schema(description = "Whether the auto-save hint has been dismissed (null=not auto-saved, 0=hint not dismissed, 1=hint dismissed)")
    private Integer autoSaveDismissed;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private LocalDateTime updatedAt;
}
