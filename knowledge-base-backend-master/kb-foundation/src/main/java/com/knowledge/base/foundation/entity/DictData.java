package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Dictionary data entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dict_data")
@Schema(description = "Dictionary data entity")
public class DictData extends BaseEntity {

    @Schema(description = "Dictionary ID")
    @TableField("dict_id")
    private Long dictId;

    @Schema(description = "Dictionary code (redundant)")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "Dictionary label")
    @TableField("dict_label")
    private String dictLabel;

    @Schema(description = "Dictionary value")
    @TableField("dict_value")
    private String dictValue;

    @Schema(description = "Sort order")
    @TableField("dict_sort")
    private Integer dictSort;

    @Schema(description = "CSS class name")
    @TableField("css_class")
    private String cssClass;

    @Schema(description = "List style")
    @TableField("list_class")
    private String listClass;

    @Schema(description = "Is default: 0-no, 1-yes")
    @TableField("is_default")
    private Integer isDefault;

    @Schema(description = "Status: 0-disabled, 1-enabled")
    @TableField("status")
    private Integer status;

}
