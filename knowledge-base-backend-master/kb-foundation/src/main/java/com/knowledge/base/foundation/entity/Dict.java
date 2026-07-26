package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Dictionary type entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dict")
@Schema(description = "Dictionary type entity")
public class Dict extends BaseEntity {

    @Schema(description = "Dictionary code")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "Dictionary name")
    @TableField("dict_name")
    private String dictName;

    @Schema(description = "Dictionary type")
    @TableField("dict_type")
    private String dictType;

    @Schema(description = "Description")
    @TableField("description")
    private String description;

    @Schema(description = "Sort order")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "Status: 0-disabled, 1-enabled")
    @TableField("status")
    private Integer status;

}
