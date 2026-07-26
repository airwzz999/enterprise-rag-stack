package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * User entity class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; stores system user information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_user")
public class User extends BaseEntity {

    /**
     * Username
     */
    private String username;

    /**
     * Password (stored encrypted)
     */
    private String password;

    /**
     * Email
     */
    private String email;

    /**
     * Whether the email has been verified (0-not verified, 1-verified)
     */
    private Integer emailVerified;

    /**
     * Account activation token
     */
    private String activationToken;

    /**
     * Activation token expiry time
     */
    private LocalDateTime activationTokenExpiry;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar URL
     */
    private String avatar;

    /**
     * Real name
     */
    @TableField("real_name")
    private String realName;

    /**
     * Department
     */
    @TableField("department")
    private String department;

    /**
     * Position
     */
    @TableField("position")
    private String position;

    /**
     * Remark/bio
     */
    private String remark;

    /**
     * Status (0-disabled, 1-enabled)
     */
    private Integer status;

    /**
     * Last login time
     */
    private LocalDateTime lastLoginTime;

    /**
     * Last login IP
     */
    private String lastLoginIp;

    /**
     * Whether deleted (0-no, 1-yes)
     */
    @TableField(exist = false)
    private Integer isDeleted;
}
