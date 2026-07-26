package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.CategoryDTO;
import com.knowledge.base.document.entity.Category;
import com.knowledge.base.document.mapper.CategoryMapper;
import com.knowledge.base.document.service.CategoryService;
import com.knowledge.base.document.vo.CategoryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Category Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements category related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:categories", allEntries = true)
    public Long createCategory(CategoryDTO categoryDTO) {
        log.info("Create category: categoryName={}", categoryDTO.getName());

        // Check whether the category name already exists
        Category existCategory = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getCategoryName, categoryDTO.getName())
        );
        if (existCategory != null) {
            throw new BusinessException("Category name already exists");
        }

        // Check whether the parent category exists
        Long parentId = categoryDTO.getParentId() != null ? categoryDTO.getParentId() : 0L;
        if (parentId > 0) {
            Category parentCategory = categoryMapper.selectById(parentId);
            if (parentCategory == null) {
                throw new BusinessException("Parent category does not exist");
            }
        }

        // Generate the category code
        String categoryCode = StringUtils.hasText(categoryDTO.getName())
                ? generateCategoryCode(categoryDTO.getName())
                : "CATEGORY_" + System.currentTimeMillis();

        // Build the category entity
        Category category = new Category();
        category.setId(SnowflakeIdGenerator.getInstance().nextId());
        category.setParentId(parentId);
        category.setCategoryName(categoryDTO.getName());
        category.setCategoryCode(categoryCode);
        category.setDescription(categoryDTO.getDescription());
        category.setIcon(categoryDTO.getIcon());
        category.setSort(categoryDTO.getSortOrder() != null ? categoryDTO.getSortOrder() : 0);
        category.setStatus(1);
        category.setDocumentCount(0);

        // Save the category
        int count = categoryMapper.insert(category);
        if (count <= 0) {
            throw new BusinessException("Failed to create category");
        }

        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:categories", allEntries = true)
    public Boolean updateCategory(CategoryDTO categoryDTO) {
        log.info("Update category: categoryId={}", categoryDTO.getId());

        if (categoryDTO.getId() == null) {
            throw new BusinessException("Category ID must not be null");
        }

        // Check whether the category exists
        Category existCategory = categoryMapper.selectById(categoryDTO.getId());
        if (existCategory == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether the category name is used by another category
        if (StringUtils.hasText(categoryDTO.getName())
                && !categoryDTO.getName().equals(existCategory.getCategoryName())) {
            Category category = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getCategoryName, categoryDTO.getName())
            );
            if (category != null && !category.getId().equals(categoryDTO.getId())) {
                throw new BusinessException("Category name is already in use");
            }
        }

        // Check whether the parent category exists
        if (categoryDTO.getParentId() != null) {
            if (categoryDTO.getParentId().equals(categoryDTO.getId())) {
                throw new BusinessException("The parent category cannot be itself");
            }
            if (categoryDTO.getParentId() > 0) {
                Category parentCategory = categoryMapper.selectById(categoryDTO.getParentId());
                if (parentCategory == null) {
                    throw new BusinessException("Parent category does not exist");
                }
            }
        }

        // Build the update entity
        Category category = new Category();
        category.setId(categoryDTO.getId());
        if (StringUtils.hasText(categoryDTO.getName())) {
            category.setCategoryName(categoryDTO.getName());
        }
        category.setDescription(categoryDTO.getDescription());
        if (categoryDTO.getParentId() != null) {
            category.setParentId(categoryDTO.getParentId());
        }
        if (categoryDTO.getIcon() != null) {
            category.setIcon(categoryDTO.getIcon());
        }
        if (categoryDTO.getSortOrder() != null) {
            category.setSort(categoryDTO.getSortOrder());
        }

        int count = categoryMapper.updateById(category);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:categories", allEntries = true)
    public Boolean deleteCategory(Long categoryId) {
        log.info("Delete category: categoryId={}", categoryId);

        if (categoryId == null) {
            throw new BusinessException("Category ID must not be null");
        }

        // Check whether the category exists
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether there are subcategories
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, categoryId)
        );
        if (childCount > 0) {
            throw new BusinessException("This category has subcategories and cannot be deleted");
        }

        // TODO: check whether there are associated documents; if so, disallow deletion or prompt the user

        // Delete the category
        int count = categoryMapper.deleteById(categoryId);
        return count > 0;
    }

    @Override
    public CategoryVO getCategoryById(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("Category ID must not be null");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        return convertToVO(category);
    }

    @Override
    @Cacheable(value = "categoryTree", key = "'tree'", cacheManager = "caffeineCacheManager")
    public List<CategoryVO> getCategoryTree() {
        // Query all categories
        List<Category> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        // Convert to VO
        List<CategoryVO> categoryVOs = allCategories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // Build the tree structure
        return buildCategoryTree(categoryVOs, 0L);
    }

    @Override
    public List<CategoryVO> getChildren(Long parentId) {
        if (parentId == null) {
            parentId = 0L;
        }

        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, parentId)
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:categories", allEntries = true)
    public Boolean moveCategory(Long categoryId, Long newParentId) {
        log.info("Move category: categoryId={}, newParentId={}", categoryId, newParentId);

        if (categoryId == null) {
            throw new BusinessException("Category ID must not be null");
        }

        // Check whether the category exists
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether moving to itself
        if (categoryId.equals(newParentId)) {
            throw new BusinessException("Cannot move to itself");
        }

        // Check whether the new parent category exists
        if (newParentId != null && newParentId > 0) {
            Category parentCategory = categoryMapper.selectById(newParentId);
            if (parentCategory == null) {
                throw new BusinessException("Parent category does not exist");
            }

            // Check whether moving under its own subcategory
            if (isDescendant(categoryId, newParentId)) {
                throw new BusinessException("Cannot move under one of its own subcategories");
            }
        }

        // Update the parent category ID
        Category updateCategory = new Category();
        updateCategory.setId(categoryId);
        updateCategory.setParentId(newParentId != null ? newParentId : 0L);

        int count = categoryMapper.updateById(updateCategory);
        return count > 0;
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * Converts to VO
     *
     * @param category category entity
     * @return category VO
     */
    private CategoryVO convertToVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getCategoryName())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .sortOrder(category.getSort())
                .icon(category.getIcon())
                .documentCount(category.getDocumentCount() != null ? category.getDocumentCount().longValue() : 0L)
                .build();
    }

    /**
     * Builds the category tree
     *
     * @param categories category list
     * @param parentId   parent category ID
     * @return category tree
     */
    private List<CategoryVO> buildCategoryTree(List<CategoryVO> categories, Long parentId) {
        List<CategoryVO> tree = new ArrayList<>();

        for (CategoryVO category : categories) {
            if (parentId.equals(category.getParentId())) {
                // Recursively find subcategories
                category.setChildren(buildCategoryTree(categories, category.getId()));
                tree.add(category);
            }
        }

        return tree;
    }

    /**
     * Checks whether a node is a descendant
     *
     * @param ancestorId ancestor node ID
     * @param descendantId descendant node ID
     * @return whether it is a descendant
     */
    private boolean isDescendant(Long ancestorId, Long descendantId) {
        Category category = categoryMapper.selectById(descendantId);
        while (category != null && category.getParentId() != null && category.getParentId() > 0) {
            if (category.getParentId().equals(ancestorId)) {
                return true;
            }
            category = categoryMapper.selectById(category.getParentId());
        }
        return false;
    }

    /**
     * Generates a category code
     *
     * @param categoryName category name
     * @return category code
     */
    private String generateCategoryCode(String categoryName) {
        // Simple pinyin-initial or abbreviation generation logic
        // A pinyin conversion library could be used in a real project
        return "CAT_" + categoryName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "") + "_" + System.currentTimeMillis();
    }
}
