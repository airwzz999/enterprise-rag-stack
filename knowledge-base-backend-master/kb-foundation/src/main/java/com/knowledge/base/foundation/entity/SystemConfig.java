package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System configuration entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_system_config")
@Schema(description = "System configuration entity")
public class SystemConfig extends BaseEntity {

    @Schema(description = "Config key")
    @TableField("config_key")
    private String configKey;

    @Schema(description = "Config value")
    @TableField("config_value")
    private String configValue;

    @Schema(description = "Config type: string/number/boolean/json")
    @TableField("config_type")
    private String configType;

    @Schema(description = "Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.")
    @TableField("category")
    private String category;

    @Schema(description = "Config description")
    @TableField("description")
    private String description;

    @Schema(description = "Is public: 0-private, 1-public")
    @TableField("is_public")
    private Integer isPublic;

}
