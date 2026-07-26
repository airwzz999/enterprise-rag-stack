package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import com.knowledge.base.document.enums.TagTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tag entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_tag")
@Schema(description = "Tag entity")
public class Tag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Tag ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Tag ID")
    private Long id;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    private String tagName;

    /**
     * Tag code
     */
    @Schema(description = "Tag code")
    private String tagCode;

    /**
     * Parent category ID
     */
    @Schema(description = "Parent category ID")
    private Long categoryId;

    /**
     * Tag type: 0-SYSTEM, 1-USER
     */
    @Schema(description = "Tag type")
    private Integer tagType;

    /**
     * Color
     */
    @Schema(description = "Color")
    private String color;

    /**
     * Icon
     */
    @Schema(description = "Icon")
    private String icon;

    /**
     * Document count
     */
    @Schema(description = "Document count")
    private Integer docCount;

    /**
     * Status: 0-disabled, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Delete flag
     */
    @TableLogic
    @Schema(description = "Delete flag")
    private Integer deleted;

    /**
     * Gets the tag type enum
     *
     * @return tag type enum
     */
    public TagTypeEnum getTagTypeEnum() {
        return TagTypeEnum.of(this.tagType);
    }

    /**
     * Sets the tag type (enum)
     *
     * @param tagTypeEnum tag type enum
     */
    public void setTagTypeEnum(TagTypeEnum tagTypeEnum) {
        if (tagTypeEnum != null) {
            this.tagType = tagTypeEnum.getCode();
        }
    }
}
