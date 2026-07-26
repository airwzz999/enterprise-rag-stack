package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Category Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * Queries the subcategory list by parent category ID
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * Queries a category by category code
     */
    Category selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * Queries the category list by status
     */
    List<Category> selectByStatus(@Param("status") Integer status);

    /**
     * Queries all root categories
     */
    List<Category> selectRootCategories();

    /**
     * Updates the document count
     */
    int updateDocumentCount(@Param("categoryId") Long categoryId, @Param("count") Integer count);

    /**
     * Increments the document count
     */
    int incrementDocumentCount(@Param("categoryId") Long categoryId);

    /**
     * Decrements the document count (not below 0)
     */
    int decrementDocumentCount(@Param("categoryId") Long categoryId);

    /**
     * Checks whether a category code already exists
     */
    boolean checkCategoryCodeExists(@Param("categoryCode") String categoryCode, @Param("id") Long id);
}
