package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role entity class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; stores system role information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_role")
public class Role extends BaseEntity {

    /**
     * Role name
     */
    private String roleName;

    /**
     * Role code
     */
    private String roleCode;

    /**
     * Role description
     */
    private String description;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Status (0-disabled, 1-enabled)
     */
    private Integer status;

    /**
     * Remark
     */
    private String remark;
}
