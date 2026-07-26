package com.knowledge.base.common.config;

import com.baomidou.mybatisplus.annotation.*;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; all entity classes should extend this class</p>
 * <p>Includes common fields: ID, creation time, update time, creator, updater, and a logical-delete flag</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>ID: generated with the Snowflake algorithm to guarantee distributed uniqueness</li>
 *   <li>Logical delete: uses the @TableLogic annotation, handled automatically by MyBatis Plus</li>
 *   <li>Field auto-fill: uses FieldFill together with MetaObjectHandler</li>
 *   <li>No version field: optimistic locking is not required by every table, so add it as needed</li>
 * </ul>
 *
 * <p>If optimistic locking is required:</p>
 * <ul>
 *   <li>Option 1: add an @Version private Integer version field to the specific entity class</li>
 *   <li>Option 2: extend the BaseEntityWithVersion class (recommended)</li>
 *   <li>Note: the database table must also have a version column added (INT, default 0)</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID (generated with the Snowflake algorithm)
     */
    @TableId(type = IdType.INPUT)
    private Long id;

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
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * Updater ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * Logical delete flag (0 - not deleted, 1 - deleted)
     */
    @TableLogic
    private Integer deleted;

    /**
     * Auto-fill the ID before insert
     */
    public void preInsert() {
        if (this.id == null) {
            this.id = SnowflakeIdGenerator.getInstance().nextId();
        }
    }
}
