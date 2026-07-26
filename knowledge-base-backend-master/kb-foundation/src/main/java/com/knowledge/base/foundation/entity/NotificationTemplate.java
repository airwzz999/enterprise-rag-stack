package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Notification template entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_notification_template")
@Schema(description = "Notification template entity")
public class NotificationTemplate extends BaseEntity {

    @Schema(description = "Template code")
    @TableField("template_code")
    private String templateCode;

    @Schema(description = "Template name")
    @TableField("template_name")
    private String templateName;

    @Schema(description = "Notification type: EMAIL/SMS/WECHAT/SYSTEM/BROWSER")
    @TableField("notification_type")
    private String notificationType;

    @Schema(description = "Template title")
    @TableField("title")
    private String title;

    @Schema(description = "Template content")
    @TableField("content")
    private String content;

    @Schema(description = "Template variables (JSON array)")
    @TableField("variables")
    private String variables;

    @Schema(description = "Template description")
    @TableField("description")
    private String description;

    @Schema(description = "Is active: 0-inactive, 1-active")
    @TableField("is_active")
    private Integer isActive;

}
