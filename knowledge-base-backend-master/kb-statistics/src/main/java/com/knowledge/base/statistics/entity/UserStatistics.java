package com.knowledge.base.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User statistics entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_user")
public class UserStatistics {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Username
     */
    private String username;

    /**
     * Real name
     */
    private String realName;

    /**
     * Avatar
     */
    private String avatar;

    /**
     * Status
     */
    private Integer status;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;
}
