package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User-role association entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("kb_user_role")
public class UserRole {

    /**
     * Primary key ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Role ID
     */
    private Long roleId;

    /**
     * Created at
     */
    private LocalDateTime createdAt;

    /**
     * Created by
     */
    private Long createBy;
}
