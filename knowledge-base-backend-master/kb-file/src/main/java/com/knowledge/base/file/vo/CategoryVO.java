package com.knowledge.base.file.vo;

import com.knowledge.base.file.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * File category VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "File category response")
public class CategoryVO {

    /**
     * Category ID
     */
    @Schema(description = "Category ID")
    private Long id;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String name;

    /**
     * Parent category ID
     */
    @Schema(description = "Parent category ID")
    private Long parentId;

    /**
     * Category level
     */
    @Schema(description = "Category level")
    private Integer level;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sortOrder;

    /**
     * Category icon
     */
    @Schema(description = "Category icon")
    private String icon;

    /**
     * Category description
     */
    @Schema(description = "Category description")
    private String description;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Child category list
     */
    @Schema(description = "Child category list")
    private List<CategoryVO> children = new ArrayList<>();

    /**
     * Convert an entity to a VO
     *
     * @param entity category entity
     * @return category VO
     */
    public static CategoryVO fromEntity(Category entity) {
        if (entity == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setParentId(entity.getParentId());
        vo.setLevel(entity.getLevel());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIcon(entity.getIcon());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
