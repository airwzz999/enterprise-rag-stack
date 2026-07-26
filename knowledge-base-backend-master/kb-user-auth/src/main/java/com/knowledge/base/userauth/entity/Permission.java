package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Permission entity class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; stores system permission information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_permission")
public class Permission extends BaseEntity {

    /**
     * Parent permission ID
     */
    private Long parentId;

    /**
     * Permission name
     */
    private String permissionName;

    /**
     * Permission code
     */
    private String permissionCode;

    /**
     * Permission type (1-menu, 2-button)
     */
    private Integer permissionType;

    /**
     * Menu URL
     */
    @TableField("menu_url")
    private String menuUrl;

    /**
     * API URL
     */
    @TableField("api_url")
    private String apiUrl;

    /**
     * Request method
     */
    @TableField("method")
    private String method;

    /**
     * Icon
     */
    private String icon;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Status (0-disabled, 1-enabled)
     */
    private Integer status;
}
