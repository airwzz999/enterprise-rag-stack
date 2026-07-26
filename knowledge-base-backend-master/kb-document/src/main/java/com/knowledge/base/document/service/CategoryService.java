package com.knowledge.base.document.service;

import com.knowledge.base.document.dto.CategoryDTO;
import com.knowledge.base.document.vo.CategoryVO;

import java.util.List;

/**
 * Category Service interface
 *
 * <p>Provides document category related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CategoryService {

    /**
     * Creates a category
     *
     * @param categoryDTO category information
     * @return category ID
     */
    Long createCategory(CategoryDTO categoryDTO);

    /**
     * Updates a category
     *
     * @param categoryDTO category information
     * @return whether successful
     */
    Boolean updateCategory(CategoryDTO categoryDTO);

    /**
     * Deletes a category
     *
     * @param categoryId category ID
     * @return whether successful
     */
    Boolean deleteCategory(Long categoryId);

    /**
     * Queries a category by ID
     *
     * @param categoryId category ID
     * @return category information
     */
    CategoryVO getCategoryById(Long categoryId);

    /**
     * Gets the category tree
     *
     * @return category tree
     */
    List<CategoryVO> getCategoryTree();

    /**
     * Gets subcategories
     *
     * @param parentId parent category ID
     * @return subcategory list
     */
    List<CategoryVO> getChildren(Long parentId);

    /**
     * Moves a category
     *
     * @param categoryId   category ID
     * @param newParentId new parent category ID
     * @return whether successful
     */
    Boolean moveCategory(Long categoryId, Long newParentId);

    /**
     * Gets all categories
     *
     * @return category list
     */
    List<CategoryVO> getAllCategories();
}
