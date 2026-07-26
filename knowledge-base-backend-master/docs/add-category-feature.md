# Adding the Category Feature

## I. Overview

Document categorization is a core organizational feature of the knowledge base system, enabling scientific management and fast retrieval of documents through a tree-structured category hierarchy. This article describes in detail how to implement a complete category management feature in the kb-document service.

### 1.1 Feature Positioning

Category management, as the infrastructure for organizing documents, provides the following core features:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Category CRUD | Create, update, delete, and query categories | Basic category management |
| Tree structure | Build a category tree with parent-child relationships | Hierarchical display |
| Category move | Move a category under a different parent category | Reorganizing structure |
| Document statistics | Count the documents under a category | Data display |
| Batch operations | Bulk enable/disable categories | Quick management |

### 1.2 Data Model

Categories use the classic tree structure design:

```
Root category (parentId = 0)
├── Technical Documentation
│   ├── Backend Development
│   │   ├── Java
│   │   └── Spring Boot
│   └── Frontend Development
│       ├── Vue.js
│       └── React
├── Product Documentation
│   ├── PRD Documents
│   └── User Manuals
└── Operations Documentation
    ├── Deployment Docs
    └── Monitoring Docs
```

### 1.3 Technical Implementation

- **Tree structure construction**: a recursive algorithm builds the parent-child mapping
- **Circular reference detection**: prevents moving a category under its own descendant
- **Cascade checking**: checks for child categories and associated documents before deletion
- **Document count statistics**: keeps the document count under each category up to date in real time

---

## II. Database Design

### 2.1 Category Table Structure

```sql
CREATE TABLE `kb_category` (
  `id` BIGINT NOT NULL COMMENT 'Category ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent category ID (0 means root category)',
  `category_name` VARCHAR(50) NOT NULL COMMENT 'Category name',
  `category_code` VARCHAR(50) NOT NULL COMMENT 'Category code',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Category description',
  `icon` VARCHAR(50) DEFAULT '📁' COMMENT 'Category icon',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
  `document_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remarks',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_category_code` (`category_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document category table';
```

### 2.2 Field Descriptions

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Category ID | Yes | Auto-increment |
| parent_id | BIGINT | Parent category ID | Yes | 0 |
| category_name | VARCHAR(50) | Category name | Yes | - |
| category_code | VARCHAR(50) | Category code | Yes | - |
| description | VARCHAR(500) | Category description | No | - |
| icon | VARCHAR(50) | Category icon | No | 📁 |
| sort | INT | Sort order | Yes | 0 |
| status | TINYINT | Status | Yes | 1 |
| document_count | INT | Document count | Yes | 0 |

### 2.3 Index Design

- **Primary key index**: `id` - uniquely identifies the primary key
- **Parent index**: `idx_parent_id` - speeds up queries for child categories
- **Code index**: `idx_category_code` - speeds up queries by code
- **Status index**: `idx_status` - speeds up filtering by status

---

## III. Entity Class Design

### 3.1 Create the Category Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/Category.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Document category entity class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; stores
 * document category information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_category")
public class Category extends BaseEntity {

    /**
     * Parent category ID (0 means root category)
     */
    private Long parentId;

    /**
     * Category name
     */
    private String categoryName;

    /**
     * Category code
     */
    private String categoryCode;

    /**
     * Category description
     */
    private String description;

    /**
     * Icon
     */
    private String icon;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Status (0-disabled, 1-enabled)
     */
    private Integer status;

    /**
     * Document count
     */
    private Integer documentCount;

    /**
     * Remarks
     */
    private String remark;
}
```

### 3.2 Design Approach

1. **Extends BaseEntity**: reuses base fields (id, creation time, update time, etc.)
2. **parentId field**: implements the tree structure; 0 means the root category
3. **documentCount field**: a denormalized design that avoids frequent join queries
4. **status field**: supports soft deletion and enable/disable functionality

---

## IV. DTO and VO Design

### 4.1 Create CategoryDTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/CategoryDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Category DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Category information")
public class CategoryDTO {

    @Schema(description = "Category ID")
    private Long id;

    @NotBlank(message = "Category name must not be empty")
    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID")
    private Long parentId;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Icon")
    private String icon;
}
```

### 4.2 Create CategoryVO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/CategoryVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Category VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category information")
public class CategoryVO {

    @Schema(description = "Category ID")
    private Long id;

    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Parent category ID")
    private Long parentId;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Icon")
    private String icon;

    @Schema(description = "Document count")
    private Long documentCount;

    @Schema(description = "List of child categories")
    private List<CategoryVO> children;
}
```

### 4.3 Design Notes

**Differences between DTO and VO:**

| Type | Purpose | Characteristics |
|------|------|------|
| DTO | Receives client requests | Trimmed-down fields, used for parameter validation |
| VO | Returns client responses | Includes computed business fields (such as children) |

**Conversion logic:**
- `name` in the DTO maps to `categoryName` in the Entity
- `children` in the VO is generated dynamically by the tree-building algorithm

---

## V. Data Access Layer

### 5.1 Create the Mapper Interface

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/CategoryMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * Category Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

}
```

### 5.2 Create the Mapper XML

Create `kb-document/src/main/resources/mapper/CategoryMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.CategoryMapper">

    <!-- Common query result mapping -->
    <resultMap id="BaseResultMap" type="com.knowledge.base.document.entity.Category">
        <id column="id" property="id" />
        <result column="parent_id" property="parentId" />
        <result column="category_name" property="categoryName" />
        <result column="category_code" property="categoryCode" />
        <result column="description" property="description" />
        <result column="icon" property="icon" />
        <result column="sort" property="sort" />
        <result column="status" property="status" />
        <result column="document_count" property="documentCount" />
        <result column="remark" property="remark" />
        <result column="deleted" property="deleted" />
        <result column="create_time" property="createTime" />
        <result column="update_time" property="updateTime" />
        <result column="create_by" property="createBy" />
        <result column="update_by" property="updateBy" />
    </resultMap>

    <!-- Common query result columns -->
    <sql id="Base_Column_List">
        id, parent_id, category_name, category_code, description, icon, sort, status,
        document_count, remark, deleted, create_time, update_time, create_by, update_by
    </sql>

    <!-- Query the child category list by parent category ID -->
    <select id="selectByParentId" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List" />
        FROM kb_category
        WHERE parent_id = #{parentId}
        AND deleted = 0
        ORDER BY sort ASC, create_time DESC
    </select>

    <!-- Update the document count -->
    <update id="updateDocumentCount">
        UPDATE kb_category
        SET document_count = #{count}
        WHERE id = #{categoryId}
        AND deleted = 0
    </update>

    <!-- Increment the document count -->
    <update id="incrementDocumentCount">
        UPDATE kb_category
        SET document_count = document_count + 1
        WHERE id = #{categoryId}
        AND deleted = 0
    </update>

    <!-- Decrement the document count -->
    <update id="decrementDocumentCount">
        UPDATE kb_category
        SET document_count = GREATEST(document_count - 1, 0)
        WHERE id = #{categoryId}
        AND deleted = 0
    </update>
</mapper>
```

### 5.3 MyBatis Plus Integration

With MyBatis Plus, simple CRUD operations require no SQL to be written:

```java
// SQL is generated automatically
categoryMapper.selectById(id);
categoryMapper.selectList(wrapper);
categoryMapper.insert(category);
categoryMapper.updateById(category);
categoryMapper.deleteById(id);
```

---

## VI. Business Logic Layer

### 6.1 Create the Service Interface

Create `kb-document/src/main/java/com/knowledge/base/document/service/CategoryService.java`:

```java
package com.knowledge.base.document.service;

import com.knowledge.base.document.dto.CategoryDTO;
import com.knowledge.base.document.vo.CategoryVO;

import java.util.List;

/**
 * Category Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CategoryService {

    /**
     * Create a category
     *
     * @param categoryDTO the category information
     * @return the category ID
     */
    Long createCategory(CategoryDTO categoryDTO);

    /**
     * Update a category
     *
     * @param categoryDTO the category information
     * @return whether it succeeded
     */
    Boolean updateCategory(CategoryDTO categoryDTO);

    /**
     * Delete a category
     *
     * @param categoryId the category ID
     * @return whether it succeeded
     */
    Boolean deleteCategory(Long categoryId);

    /**
     * Query a category by ID
     *
     * @param categoryId the category ID
     * @return the category information
     */
    CategoryVO getCategoryById(Long categoryId);

    /**
     * Get the category tree
     *
     * @return the category tree
     */
    List<CategoryVO> getCategoryTree();

    /**
     * Get child categories
     *
     * @param parentId the parent category ID
     * @return the list of child categories
     */
    List<CategoryVO> getChildren(Long parentId);

    /**
     * Move a category
     *
     * @param categoryId   the category ID
     * @param newParentId the new parent category ID
     * @return whether it succeeded
     */
    Boolean moveCategory(Long categoryId, Long newParentId);

    /**
     * Get all categories
     *
     * @return the category list
     */
    List<CategoryVO> getAllCategories();
}
```

### 6.2 Create the Service Implementation Class

Create `kb-document/src/main/java/com/knowledge/base/document/service/impl/CategoryServiceImpl.java`:

```java
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Category Service implementation class
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
    public Boolean updateCategory(CategoryDTO categoryDTO) {
        log.info("Update category: categoryId={}", categoryDTO.getId());

        if (categoryDTO.getId() == null) {
            throw new BusinessException("Category ID must not be empty");
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
    public Boolean deleteCategory(Long categoryId) {
        log.info("Delete category: categoryId={}", categoryId);

        if (categoryId == null) {
            throw new BusinessException("Category ID must not be empty");
        }

        // Check whether the category exists
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether it has child categories
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, categoryId)
        );
        if (childCount > 0) {
            throw new BusinessException("This category has child categories and cannot be deleted");
        }

        // TODO: check whether it has associated documents; if so, disallow deletion or prompt the user

        // Delete the category
        int count = categoryMapper.deleteById(categoryId);
        return count > 0;
    }

    @Override
    public CategoryVO getCategoryById(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("Category ID must not be empty");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        return convertToVO(category);
    }

    @Override
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
    public Boolean moveCategory(Long categoryId, Long newParentId) {
        log.info("Move category: categoryId={}, newParentId={}", categoryId, newParentId);

        if (categoryId == null) {
            throw new BusinessException("Category ID must not be empty");
        }

        // Check whether the category exists
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("Category does not exist");
        }

        // Check whether it's being moved to itself
        if (categoryId.equals(newParentId)) {
            throw new BusinessException("Cannot move a category to itself");
        }

        // Check whether the new parent category exists
        if (newParentId != null && newParentId > 0) {
            Category parentCategory = categoryMapper.selectById(newParentId);
            if (parentCategory == null) {
                throw new BusinessException("Parent category does not exist");
            }

            // Check whether it's being moved under its own descendant
            if (isDescendant(categoryId, newParentId)) {
                throw new BusinessException("Cannot move a category under its own descendant");
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
     * Convert to VO
     *
     * @param category the category entity
     * @return the category VO
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
     * Build the category tree
     *
     * @param categories the category list
     * @param parentId   the parent category ID
     * @return the category tree
     */
    private List<CategoryVO> buildCategoryTree(List<CategoryVO> categories, Long parentId) {
        List<CategoryVO> tree = new ArrayList<>();

        for (CategoryVO category : categories) {
            if (parentId.equals(category.getParentId())) {
                // Recursively find the child categories
                category.setChildren(buildCategoryTree(categories, category.getId()));
                tree.add(category);
            }
        }

        return tree;
    }

    /**
     * Check whether a node is a descendant
     *
     * @param ancestorId   the ancestor node ID
     * @param descendantId the descendant node ID
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
     * Generate a category code
     *
     * @param categoryName the category name
     * @return the category code
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
```

### 6.3 Core Algorithm Notes

#### 6.3.1 Tree Structure Construction

```java
private List<CategoryVO> buildCategoryTree(List<CategoryVO> categories, Long parentId) {
    List<CategoryVO> tree = new ArrayList<>();

    for (CategoryVO category : categories) {
        if (parentId.equals(category.getParentId())) {
            // Recursively find the child categories
            category.setChildren(buildCategoryTree(categories, category.getId()));
            tree.add(category);
        }
    }

    return tree;
}
```

**Algorithm characteristics:**
- Time complexity: O(n²), where n is the number of nodes
- Space complexity: O(n), the recursion stack depth
- Suitable for: small-to-medium-sized category trees (<1000 nodes)

#### 6.3.2 Circular Reference Detection

```java
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
```

**Detection logic:**
1. Walk upward from the target node through its parent nodes
2. If the source node is encountered, a cycle would be formed
3. If the root is reached without encountering it, the move is safe

---

## VII. Controller Layer

### 7.1 Create CategoryController

Create `kb-document/src/main/java/com/knowledge/base/document/controller/CategoryController.java`:

```java
package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.CategoryDTO;
import com.knowledge.base.document.service.CategoryService;
import com.knowledge.base.document.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/categories")
@Tag(name = "Category Management", description = "Document category management endpoints")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * Create a category
     *
     * @param categoryDTO the category information
     * @return the category ID
     */
    @PostMapping
    @Operation(summary = "Create category", description = "Create a new category")
    public Result<Long> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Create category request: name={}", categoryDTO.getName());

        Long categoryId = categoryService.createCategory(categoryDTO);
        return Result.success("Category created successfully", categoryId);
    }

    /**
     * Update a category
     *
     * @param categoryDTO the category information
     * @return whether it succeeded
     */
    @PutMapping
    @Operation(summary = "Update category", description = "Update category information")
    public Result<Boolean> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Update category request: categoryId={}", categoryDTO.getId());

        Boolean success = categoryService.updateCategory(categoryDTO);
        return Result.success("Category updated successfully", success);
    }

    /**
     * Delete a category
     *
     * @param categoryId the category ID
     * @return whether it succeeded
     */
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete category", description = "Delete a category by category ID")
    public Result<Boolean> deleteCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId) {
        log.info("Delete category request: categoryId={}", categoryId);

        Boolean success = categoryService.deleteCategory(categoryId);
        return Result.success("Category deleted successfully", success);
    }

    /**
     * Query a category by ID
     *
     * @param categoryId the category ID
     * @return the category information
     */
    @GetMapping("/{categoryId}")
    @Operation(summary = "Query category", description = "Query category details by category ID")
    public Result<CategoryVO> getCategoryById(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId) {
        log.info("Query category request: categoryId={}", categoryId);

        CategoryVO categoryVO = categoryService.getCategoryById(categoryId);
        return Result.success(categoryVO);
    }

    /**
     * Get the category tree
     *
     * @return the category tree
     */
    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Get the complete category tree structure")
    public Result<List<CategoryVO>> getCategoryTree() {
        log.info("Get category tree request");

        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * Get child categories
     *
     * @param parentId the parent category ID
     * @return the list of child categories
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "Get child categories", description = "Get the list of child categories for the specified parent category")
    public Result<List<CategoryVO>> getChildren(
        @Parameter(description = "Parent category ID", required = true)
        @PathVariable Long parentId) {
        log.info("Get child categories request: parentId={}", parentId);

        List<CategoryVO> children = categoryService.getChildren(parentId);
        return Result.success(children);
    }

    /**
     * Move a category
     *
     * @param categoryId    the category ID
     * @param newParentId the new parent category ID
     * @return whether it succeeded
     */
    @PutMapping("/{categoryId}/move")
    @Operation(summary = "Move category", description = "Move a category under a new parent category")
    public Result<Boolean> moveCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId,
        @Parameter(description = "New parent category ID", required = true)
        @RequestParam Long newParentId) {
        log.info("Move category request: categoryId={}, newParentId={}", categoryId, newParentId);

        Boolean success = categoryService.moveCategory(categoryId, newParentId);
        return Result.success("Category moved successfully", success);
    }

    /**
     * Get all categories (flat list)
     *
     * @return the category list
     */
    @GetMapping("/list")
    @Operation(summary = "Get all categories", description = "Get the flat list of all categories")
    public Result<List<CategoryVO>> getAllCategories() {
        log.info("Get all categories request");

        List<CategoryVO> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }
}
```

### 7.2 RESTful API Design

| Method | Path | Description | Parameters |
|------|------|------|------|
| POST | /categories | Create a category | CategoryDTO |
| PUT | /categories | Update a category | CategoryDTO |
| DELETE | /categories/{id} | Delete a category | categoryId |
| GET | /categories/{id} | Query a category | categoryId |
| GET | /categories/tree | Get the category tree | - |
| GET | /categories/children/{parentId} | Get child categories | parentId |
| PUT | /categories/{id}/move | Move a category | categoryId, newParentId |
| GET | /categories/list | Get all categories | - |

---

## VIII. Testing and Verification

### 8.1 Initialize Test Data

Run the following SQL to insert test data:

```sql
-- Insert root categories
INSERT INTO kb_category (id, parent_id, category_name, category_code, description, icon, sort, status, document_count) VALUES
(1000000000000000001, 0, 'Technical Documentation', 'CAT_TECH_DOC', 'Technical documents', '📚', 1, 1, 0),
(1000000000000000002, 0, 'Product Documentation', 'CAT_PROD_DOC', 'Product documents', '📖', 2, 1, 0),
(1000000000000000003, 0, 'Operations Documentation', 'CAT_OPS_DOC', 'Operations documents', '⚙️', 3, 1, 0);

-- Insert child categories
INSERT INTO kb_category (id, parent_id, category_name, category_code, description, icon, sort, status, document_count) VALUES
(1000000000000000004, 1000000000000000001, 'Backend Development', 'CAT_BACKEND', 'Backend development technology', '💻', 1, 1, 0),
(1000000000000000005, 1000000000000000001, 'Frontend Development', 'CAT_FRONTEND', 'Frontend development technology', '🎨', 2, 1, 0),
(1000000000000000006, 1000000000000000002, 'PRD Documents', 'CAT_PRD', 'Product requirement documents', '📝', 1, 1, 0),
(1000000000000000007, 1000000000000000002, 'User Manuals', 'CAT_USER_MANUAL', 'User manuals', '📘', 2, 1, 0);

-- Insert third-level categories
INSERT INTO kb_category (id, parent_id, category_name, category_code, description, icon, sort, status, document_count) VALUES
(1000000000000000008, 1000000000000000004, 'Java', 'CAT_JAVA', 'Java programming language', '☕', 1, 1, 0),
(1000000000000000009, 1000000000000000004, 'Spring Boot', 'CAT_SPRING_BOOT', 'Spring Boot framework', '🍃', 2, 1, 0),
(1000000000000000010, 1000000000000000005, 'Vue.js', 'CAT_VUE', 'Vue.js framework', '💚', 1, 1, 0),
(1000000000000000011, 1000000000000000005, 'React', 'CAT_REACT', 'React framework', '⚛️', 2, 1, 0);
```

### 8.2 Test Category CRUD

#### Create a Root Category

```bash
curl -X POST http://localhost:8082/api/document/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Category",
    "description": "This is a test category",
    "parentId": 0,
    "sortOrder": 99,
    "icon": "🧪"
  }'
```

#### Create a Child Category

```bash
curl -X POST http://localhost:8082/api/document/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Child Category",
    "description": "This is a test child category",
    "parentId": 1000000000000000001,
    "sortOrder": 1
  }'
```

#### Query the Category Tree

```bash
curl http://localhost:8082/api/document/categories/tree
```

#### Query Child Categories

```bash
curl http://localhost:8082/api/document/categories/children/1000000000000000001
```

#### Update a Category

```bash
curl -X PUT http://localhost:8082/api/document/categories \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1000000000000000001,
    "name": "Technical Documentation (Updated)",
    "description": "Updated description"
  }'
```

#### Move a Category

```bash
curl -X PUT "http://localhost:8082/api/document/categories/1000000000000000004/move?newParentId=1000000000000000002"
```

#### Delete a Category

```bash
curl -X DELETE http://localhost:8082/api/document/categories/1000000000000000004
```

### 8.3 Verify the Data Structure

After successful execution, querying the category tree should return a structure like this:

```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": [
    {
      "id": 1000000000000000001,
      "name": "Technical Documentation",
      "description": "Technical documents",
      "parentId": 0,
      "sortOrder": 1,
      "icon": "📚",
      "documentCount": 0,
      "children": [
        {
          "id": 1000000000000000004,
          "name": "Backend Development",
          "parentId": 1000000000000000001,
          "children": [
            {
              "id": 1000000000000000008,
              "name": "Java",
              "parentId": 1000000000000000004,
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

---

## IX. Advanced Features

### 9.1 Batch Operations

Create batch operation endpoints:

```java
/**
 * Batch-delete categories
 */
@DeleteMapping("/batch")
@Operation(summary = "Batch-delete categories", description = "Batch-delete the specified categories")
public Result<Boolean> batchDelete(@RequestBody List<Long> categoryIds) {
    Boolean success = categoryService.batchDelete(categoryIds);
    return Result.success("Batch delete succeeded", success);
}

/**
 * Batch-update category status
 */
@PutMapping("/batch/status")
@Operation(summary = "Batch-update status", description = "Batch enable/disable categories")
public Result<Boolean> batchUpdateStatus(
    @RequestBody List<Long> categoryIds,
    @RequestParam Integer status) {
    Boolean success = categoryService.batchUpdateStatus(categoryIds, status);
    return Result.success("Batch update succeeded", success);
}
```

### 9.2 Automatic Document Count Updates

Add a listener in the document Service:

```java
/**
 * Update the category count after a document is created
 */
@EventListener
public void handleDocumentCreated(DocumentCreatedEvent event) {
    Long categoryId = event.getCategoryId();
    if (categoryId != null) {
        categoryMapper.incrementDocumentCount(categoryId);
    }
}

/**
 * Update the category count after a document is deleted
 */
@EventListener
public void handleDocumentDeleted(DocumentDeletedEvent event) {
    Long categoryId = event.getCategoryId();
    if (categoryId != null) {
        categoryMapper.decrementDocumentCount(categoryId);
    }
}
```

### 9.3 Category Path Caching

Optimize category tree query performance:

```java
/**
 * Get the category tree (with caching)
 */
@Cacheable(value = "category:tree", key = "'all'")
public List<CategoryVO> getCategoryTree() {
    // ... existing logic
}

/**
 * Clear the category cache
 */
@CacheEvict(value = "category:tree", allEntries = true)
public Boolean updateCategory(CategoryDTO categoryDTO) {
    // ... existing logic
}
```

---

## X. Frequently Asked Questions

### 10.1 Tree Structure Performance Optimization

**Problem**: when the number of categories is large (>1000), recursively building the tree structure performs poorly.

**Solution**:

1. **Optimize using a HashMap**:

```java
public List<CategoryVO> getCategoryTree() {
    List<Category> allCategories = categoryMapper.selectList(/*...*/);

    // Convert to a Map
    Map<Long, CategoryVO> categoryMap = allCategories.stream()
        .collect(Collectors.toMap(Category::getId, this::convertToVO));

    // Build the tree structure
    List<CategoryVO> tree = new ArrayList<>();
    for (CategoryVO category : categoryMap.values()) {
        if (category.getParentId() == 0) {
            tree.add(category);
        } else {
            CategoryVO parent = categoryMap.get(category.getParentId());
            if (parent != null) {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(category);
            }
        }
    }

    return tree;
}
```

2. **Use the Closure Table pattern**:

```sql
CREATE TABLE `kb_category_closure` (
    `ancestor_id` BIGINT NOT NULL COMMENT 'Ancestor ID',
    `descendant_id` BIGINT NOT NULL COMMENT 'Descendant ID',
    `depth` INT NOT NULL COMMENT 'Depth',
    PRIMARY KEY (`ancestor_id`, `descendant_id`)
);
```

### 10.2 Circular Reference Detection Optimization

**Problem**: recursively detecting circular references performs poorly with large amounts of data.

**Solution**: use path enumeration or a nested set model

```java
// Using path enumeration
@TableName("kb_category")
public class Category {
    private String path;  // "0/1/4/" means root → 1st node → 4th node
}
```

### 10.3 Category Deletion Restrictions

**Problem**: how should associated documents be handled when a category is deleted?

**Solution**:

1. **Strict mode**: disallow deletion if the category has documents
2. **Cascade mode**: delete the documents along with the category (not recommended)
3. **Migration mode**: require a target category to be specified when deleting a category

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean deleteCategory(Long categoryId, Long targetCategoryId) {
    // Check whether there are documents
    Long docCount = documentMapper.selectCount(
        new LambdaQueryWrapper<Document>()
            .eq(Document::getCategoryId, categoryId)
    );

    if (docCount > 0) {
        if (targetCategoryId == null) {
            throw new BusinessException("This category has documents; please specify a target category to migrate them to");
        }
        // Migrate the documents to the target category
        documentService.migrateDocumentsByCategory(categoryId, targetCategoryId);
    }

    // Delete the category
    return categoryMapper.deleteById(categoryId) > 0;
}
```

---

## XI. Summary

This article described in detail the complete implementation of the document category management feature, covering the following core content:

### Completed Features

| Feature | Implementation | Description |
|------|---------|------|
| Category CRUD | Service + Controller | Complete create/read/update/delete |
| Tree structure | Recursive algorithm | Builds parent-child relationships |
| Category move | Circular reference detection | Prevents illegal moves |
| Document statistics | Denormalized field | documentCount |
| Batch operations | List parameter | Batch delete and status updates |

### Technical Highlights

1. **Tree structure construction**: a recursive algorithm converts flat data into a tree structure
2. **Circular reference detection**: prevents moving a category under its own descendant
3. **Cascade checking**: checks child categories and associated documents before deletion
4. **Data conversion**: conversions between Entity, DTO, and VO
5. **Exception handling**: unified exception handling and error messages

### Future Improvements

1. Add a category caching mechanism
2. Implement drag-and-drop category sorting
3. Support category permission control
4. Add category operation logs
5. Implement category import/export

Through this article, you should now be able to grasp:
- Designing and implementing tree-structured data
- The application scenarios and optimization methods for recursive algorithms
- Advanced usage of MyBatis Plus
- RESTful API design conventions
- How to handle business exceptions

Happy building!
