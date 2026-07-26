package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Document entity class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, stores document information</p>
 *
 * <p>Note: the content field is stored in MongoDB; this entity only holds the contentId reference field</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class Document extends BaseEntity {

    /**
     * Document title
     */
    private String title;

    /**
     * Document summary
     */
    private String summary;

    /**
     * Document content (deprecated, content is stored in MongoDB)
     * Uses @TableField(exist = false) to tell MyBatis Plus to ignore this field
     */
    @TableField(exist = false)
    private String content;

    /**
     * MongoDB content ID (references the _id of the document_content table)
     */
    private String contentId;

    /**
     * Content length (character count, used for frontend word-count display)
     */
    private Integer contentLength;

    /**
     * Document type (1-article, 2-file)
     */
    private Integer documentType;

    /**
     * File path
     */
    private String filePath;

    /**
     * File size (bytes)
     */
    private Long fileSize;

    /**
     * File extension
     */
    private String fileExtension;

    /**
     * MIME type
     */
    private String mimeType;

    /**
     * Category ID
     */
    private Long categoryId;

    /**
     * Team space ID
     */
    private Long teamId;

    /**
     * Tags (comma-separated)
     */
    private String tags;

    /**
     * Status (0-draft, 1-published, 2-archived)
     */
    private Integer status;

    /**
     * Whether public (0-private, 1-public)
     */
    private Integer isPublic;

    /**
     * Whether pinned to top (0-no, 1-yes)
     */
    private Integer isTop;

    /**
     * Whether recommended (0-no, 1-yes)
     */
    private Integer isRecommend;

    /**
     * View count
     */
    private Long viewCount;

    /**
     * Like count
     */
    private Long likeCount;

    /**
     * Favorite count
     */
    private Long favoriteCount;

    /**
     * Comment count
     */
    private Long commentCount;

    /**
     * Publish time
     */
    private LocalDateTime publishTime;

    /**
     * Author ID
     */
    private Long authorId;

    /**
     * Author name
     */
    private String authorName;

    /**
     * Cover image URL
     */
    private String coverImage;

    /**
     * Source (1-original, 2-reposted, 3-translated)
     */
    private Integer source;

    /**
     * Source URL
     */
    private String sourceUrl;

    /**
     * Allow comments (0-no, 1-yes)
     */
    private Integer allowComment;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Auto-saved draft acknowledged (0-not acknowledged, 1-user chose to discard the restore)
     */
    private Integer autoSaveDismissed;

    /**
     * Remark
     */
    private String remark;
}
