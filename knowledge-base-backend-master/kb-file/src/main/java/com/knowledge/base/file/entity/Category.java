package com.knowledge.base.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * File category entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
@TableName("tb_category")
@Schema(description = "File category entity")
public class Category {

    private static final long serialVersionUID = 1L;

    /**
     * Category ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Category ID")
    private Long id;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String name;

    /**
     * Parent category ID (0 indicates a top-level category)
     */
    @Schema(description = "Parent category ID")
    private Long parentId;

    /**
     * Category level
     */
    @Schema(description = "Category level")
    private Integer level;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sortOrder;

    /**
     * Category icon
     */
    @Schema(description = "Category icon")
    private String icon;

    /**
     * Category description
     */
    @Schema(description = "Category description")
    private String description;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Deletion flag
     */
    @TableLogic
    @Schema(description = "Deletion flag")
    private Integer deleted;

    /**
     * Creation time
     */
    @TableField("created_at")
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @TableField("updated_at")
    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    /**
     * Creator ID
     */
    @Schema(description = "Creator ID")
    private Long createBy;

    /**
     * Updater ID
     */
    @Schema(description = "Updater ID")
    private Long updateBy;
}
