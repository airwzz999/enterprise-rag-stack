package com.knowledge.base.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.file.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * File category Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * Query the list of child categories by parent category ID
     *
     * @param parentId parent category ID
     * @return list of child categories
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * Query all enabled categories
     *
     * @return list of categories
     */
    List<Category> selectAllEnabled();

    /**
     * Query the IDs of a category and all of its child categories
     *
     * @param parentId parent category ID
     * @return list of category IDs
     */
    List<Long> selectChildIds(@Param("parentId") Long parentId);
}
