package com.knowledge.base.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.file.dto.CategoryDTO;
import com.knowledge.base.file.entity.Category;
import com.knowledge.base.file.mapper.CategoryMapper;
import com.knowledge.base.file.service.CategoryService;
import com.knowledge.base.file.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * File category service implementation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategoryDTO dto) {
        log.info("Creating category: {}", dto.getName());

        // Check whether the category name already exists
        checkNameExists(null, dto.getName());

        // Compute the category level
        int level = 1;
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Category parent = super.getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("Parent category does not exist");
            }
            level = parent.getLevel() + 1;
            if (level > 5) {
                throw new BusinessException("Category level must not exceed 5");
            }
        }

        // Create the category entity
        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setLevel(level);
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        save(category);
        log.info("Category created successfully: id={}, name={}", category.getId(), category.getName());

        return CategoryVO.fromEntity(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO update(CategoryDTO dto) {
        log.info("Updating category: id={}, name={}", dto.getId(), dto.getName());

        if (dto.getId() == null) {
            throw new BusinessException("Category ID must not be null");
        }

        Category category = super.getById(dto.getId());
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether the category name already exists
        checkNameExists(dto.getId(), dto.getName());

        // Update the category info
        category.setName(dto.getName());
        category.setSortOrder(dto.getSortOrder() == null ? category.getSortOrder() : dto.getSortOrder());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        category.setUpdatedAt(LocalDateTime.now());

        updateById(category);
        log.info("Category updated successfully: id={}", category.getId());

        return CategoryVO.fromEntity(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("Deleting category: id={}", id);

        Category category = super.getById(id);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether it has child categories
        List<Category> children = list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id)
                .eq(Category::getDeleted, 0));
        if (!CollectionUtils.isEmpty(children)) {
            throw new BusinessException("This category has child categories and cannot be deleted");
        }

        removeById(id);
        log.info("Category deleted successfully: id={}", id);
    }

    @Override
    public CategoryVO getById(Long id) {
        Category category = super.getById(id);
        return CategoryVO.fromEntity(category);
    }

    @Override
    public List<CategoryVO> listAll() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .eq(Category::getDeleted, 0)
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSortOrder));
        return categories.stream()
                .map(CategoryVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getTree() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .eq(Category::getDeleted, 0)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));

        return buildTree(categories);
    }

    @Override
    public List<CategoryVO> listByParentId(Long parentId) {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId == null ? 0 : parentId)
                .eq(Category::getDeleted, 0)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
        return categories.stream()
                .map(CategoryVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO updateStatus(Long id, Integer status) {
        log.info("Updating category status: id={}, status={}", id, status);

        Category category = super.getById(id);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        category.setStatus(status);
        category.setUpdatedAt(LocalDateTime.now());
        updateById(category);

        log.info("Category status updated successfully: id={}, status={}", id, status);
        return CategoryVO.fromEntity(category);
    }

    /**
     * Check whether the category name already exists
     */
    private void checkNameExists(Long excludeId, String name) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name)
                .eq(Category::getDeleted, 0);
        if (excludeId != null) {
            queryWrapper.ne(Category::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new BusinessException("Category name already exists");
        }
    }

    /**
     * Build the category tree
     */
    private List<CategoryVO> buildTree(List<Category> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return Collections.emptyList();
        }

        // Group by parent ID
        Map<Long, List<Category>> groupedByParentId = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        // Recursively build the tree
        return buildChildren(0L, groupedByParentId);
    }

    /**
     * Recursively build child nodes
     */
    private List<CategoryVO> buildChildren(Long parentId, Map<Long, List<Category>> groupedByParentId) {
        List<Category> children = groupedByParentId.get(parentId);
        if (CollectionUtils.isEmpty(children)) {
            return Collections.emptyList();
        }

        return children.stream()
                .map(category -> {
                    CategoryVO vo = CategoryVO.fromEntity(category);
                    vo.setChildren(buildChildren(category.getId(), groupedByParentId));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
