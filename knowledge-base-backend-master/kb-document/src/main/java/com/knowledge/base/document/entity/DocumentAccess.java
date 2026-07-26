package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document access record entity class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, records the history of users accessing documents</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_document_access")
public class DocumentAccess {

    /**
     * Access record ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Document ID
     */
    private Long documentId;

    /**
     * Document title
     */
    private String documentTitle;

    /**
     * Access time
     */
    private LocalDateTime accessTime;

    /**
     * Creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * Creator ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * Updater ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * Logical delete flag (0-not deleted, 1-deleted)
     */
    @TableLogic
    private Integer deleted;

    /**
     * Document summary (fetched via join at query time)
     */
    @TableField(exist = false)
    private String summary;

    /**
     * Category name (fetched via join at query time)
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * Author name (fetched via join at query time)
     */
    @TableField(exist = false)
    private String authorName;

    /**
     * Document status (fetched via join at query time)
     */
    @TableField(exist = false)
    private Integer status;
}
