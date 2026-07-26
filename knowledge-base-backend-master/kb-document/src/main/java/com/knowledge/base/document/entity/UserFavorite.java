package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User favorite entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_user_favorite")
public class UserFavorite extends BaseEntity {

    /**
     * Overrides the deleted field, disabling BaseEntity's logical delete
     * The favorite feature uses physical deletion
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Document ID
     */
    private Long documentId;

    /**
     * Document title (redundant field)
     */
    private String documentTitle;

    /**
     * Document category ID (redundant field)
     */
    private Long documentCategoryId;

    /**
     * Favorite time
     */
    private LocalDateTime favoriteTime;

    /**
     * Document summary (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private String documentSummary;

    /**
     * Document author name (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private String documentAuthorName;

    /**
     * Document author ID (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private Long documentAuthorId;

    /**
     * Document status (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private Integer documentStatus;

    /**
     * Document view count (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private Long documentViewCount;

    /**
     * Document creation time (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private LocalDateTime documentCreateTime;

    /**
     * Document update time (joined query field, not stored in the database)
     */
    @TableField(exist = false)
    private LocalDateTime documentUpdateTime;
}
