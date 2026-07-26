package com.knowledge.base.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.file.dto.CategoryDTO;
import com.knowledge.base.file.entity.Category;
import com.knowledge.base.file.vo.CategoryVO;

import java.util.List;

/**
 * File category service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CategoryService extends IService<Category> {

    /**
     * Create a category
     *
     * @param dto category request
     * @return the created category VO
     */
    CategoryVO create(CategoryDTO dto);

    /**
     * Update a category
     *
     * @param dto category request
     * @return the updated category VO
     */
    CategoryVO update(CategoryDTO dto);

    /**
     * Delete a category
     *
     * @param id category ID
     */
    void delete(Long id);

    /**
     * Get a category by ID
     *
     * @param id category ID
     * @return category VO
     */
    CategoryVO getById(Long id);

    /**
     * Get all categories (flat list)
     *
     * @return list of categories
     */
    List<CategoryVO> listAll();

    /**
     * Get the category tree structure
     *
     * @return category tree
     */
    List<CategoryVO> getTree();

    /**
     * Get the list of child categories by parent category ID
     *
     * @param parentId parent category ID
     * @return list of child categories
     */
    List<CategoryVO> listByParentId(Long parentId);

    /**
     * Enable/disable a category
     *
     * @param id     category ID
     * @param status status
     * @return the updated category VO
     */
    CategoryVO updateStatus(Long id, Integer status);
}
