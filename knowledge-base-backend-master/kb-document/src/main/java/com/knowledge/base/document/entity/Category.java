package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Document category entity class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, stores document category information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_category")
public class Category extends BaseEntity {

    /**
     * Parent category ID (0 indicates a root category)
     */
    private Long parentId;

    /**
     * Category name
     */
    private String categoryName;

    /**
     * Category code
     */
    private String categoryCode;

    /**
     * Category description
     */
    private String description;

    /**
     * Icon
     */
    @TableField("category_icon")
    private String icon;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Status (0-disabled, 1-enabled)
     */
    private Integer status;

    /**
     * Document count
     */
    private Integer documentCount;

}
