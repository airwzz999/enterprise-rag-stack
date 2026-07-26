# Adding the Tag Feature

## I. Overview

Tags are an important supplementary way of organizing documents in the knowledge base system, enabling flexible categorization and multi-dimensional retrieval of documents. This article describes in detail how to implement a complete tag management feature in the kb-document service.

### 1.1 Feature Positioning

Tag management, as a supplementary way of organizing documents, provides the following core features:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Tag CRUD | Create, update, delete, and query tags | Basic tag management |
| Popular tags | Displayed sorted by usage frequency | Popular content recommendations |
| Category association | Tags associated with categories | Filtering tags by category |
| Batch creation | Bulk-create tags | Quickly tagging content |
| Tag search | Fuzzy tag search | Autocomplete suggestions |

### 1.2 Differences Between Tags and Categories

| Characteristic | Category | Tag |
|------|------|------|
| Structure | Tree-like hierarchical structure | Flat structure |
| Quantity | Relatively fixed, pre-planned by administrators | Grows dynamically, freely created by users |
| Constraint | A document can only belong to one primary category | A document can have multiple tags |
| Purpose | Primary classification and navigation for documents | Multi-dimensional labeling and cross-cutting retrieval of documents |
| Management | Usually managed centrally by administrators | Freely created and used by users |
| Hierarchy | Supports multi-level parent-child categories | No hierarchy, flat |
| Querying | Browse by category hierarchy | Search by tag combinations |
| Typical scenario | Technical docs (Frontend/Backend/Database) | Cross-category labeling (Java/Beginner/Important) |

**Usage recommendations**:
- **Categories**: used for the primary navigation of documents; it's recommended to build a clear 2-3 level category system
- **Tags**: used for auxiliary labeling of documents; encourage users to create personalized tags for knowledge management

## I. Overview

Tags are an important supplementary way of organizing documents in the knowledge base system, enabling flexible categorization and multi-dimensional retrieval of documents. This article describes in detail how to implement a complete tag management feature in the kb-document service.

### 1.1 Feature Positioning

Tag management, as a supplementary way of organizing documents, provides the following core features:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Tag CRUD | Create, update, delete, and query tags | Basic tag management |
| Popular tags | Displayed sorted by usage frequency | Popular content recommendations |
| Category association | Tags associated with categories | Filtering tags by category |
| Batch creation | Bulk-create tags | Quickly tagging content |
| Tag search | Fuzzy tag search | Autocomplete suggestions |

### 1.2 Differences Between Tags and Categories

| Characteristic | Category | Tag |
|------|------|------|
| Structure | Tree-like hierarchical structure | Flat structure |
| Quantity | Relatively fixed | Grows dynamically |
| Constraint | A document can only belong to one category | A document can have multiple tags |
| Purpose | Primary classification of documents | Multi-dimensional labeling of documents |
| Management | Usually managed by administrators | Freely created by users |

### 1.3 Tag Type Design

- **SYSTEM**: system-preset tags, such as "Pinned", "Recommended", "Featured", etc.
- **USER**: user-defined tags; users can freely create these as needed

---

## II. Database Design

### 2.1 Tag Table Structure

```sql
CREATE TABLE `tb_tag` (
  `id` BIGINT NOT NULL COMMENT 'Tag ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT 'Tag name',
  `tag_code` VARCHAR(50) NOT NULL COMMENT 'Tag code',
  `category_id` BIGINT DEFAULT NULL COMMENT 'Owning category ID',
  `tag_type` TINYINT NOT NULL DEFAULT 1 COMMENT 'Tag type: 0-SYSTEM, 1-user tag',
  `color` VARCHAR(20) DEFAULT NULL COMMENT 'Tag color',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Tag icon',
  `doc_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  `version` INT NOT NULL DEFAULT 0 COMMENT 'Version number',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`),
  KEY `idx_tag_name` (`tag_name`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_tag_type` (`tag_type`),
  KEY `idx_status` (`status`),
  KEY `idx_doc_count` (`doc_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tag table';
```

### 2.2 Document-Tag Association Table

```sql
CREATE TABLE `kb_document_tag` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `tag_id` BIGINT NOT NULL COMMENT 'Tag ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_tag` (`document_id`, `tag_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document-tag association table';
```

### 2.3 Field Descriptions

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Tag ID | Yes | Auto-increment |
| tag_name | VARCHAR(50) | Tag name | Yes | - |
| tag_code | VARCHAR(50) | Tag code | Yes | - |
| category_id | BIGINT | Owning category ID | No | - |
| tag_type | TINYINT | Tag type: 0-SYSTEM, 1-USER | Yes | 1 |
| color | VARCHAR(20) | Tag color | No | - |
| icon | VARCHAR(50) | Tag icon | No | - |
| doc_count | INT | Document count | Yes | 0 |
| status | TINYINT | Status | Yes | 1 |

### 2.4 Index Design

- **Primary key index**: `id` - uniquely identifies the primary key
- **Unique index**: `uk_tag_code` - ensures the tag code is unique
- **Regular index**: `idx_tag_name` - speeds up queries by name
- **Regular index**: `idx_category_id` - speeds up queries by category
- **Regular index**: `idx_doc_count` - speeds up sorting by document count

---

## III. Entity Class Design

### 3.1 Create the Tag Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/Tag.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
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
     * Owning category ID
     */
    @Schema(description = "Owning category ID")
    private Long categoryId;

    /**
     * Tag type: 0-SYSTEM, 1-user tag
     */
    @Schema(description = "Tag type")
    private Integer tagType;

    /**
     * Get the tag type enum
     *
     * @return the tag type enum
     */
    public com.knowledge.base.document.enums.TagTypeEnum getTagTypeEnum() {
        return com.knowledge.base.document.enums.TagTypeEnum.of(this.tagType);
    }

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
}
```

### 3.2 Design Notes

1. **Extends BaseEntity**: reuses base fields (creation time, update time, etc.)
2. **tagCode uniqueness**: a unique index ensures the tag code is never duplicated
3. **docCount field**: a denormalized design that avoids frequent join queries
4. **tagType field**: distinguishes system tags from user tags

---

## IV. DTO and VO Design

### 4.1 Create TagCreateDTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/TagCreateDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Tag creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Tag creation request")
public class TagCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    @NotBlank(message = "Tag name must not be empty")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String tagName;

    /**
     * Tag code
     */
    @Schema(description = "Tag code")
    @Size(max = 50, message = "Tag code must not exceed 50 characters")
    private String tagCode;

    /**
     * Owning category ID
     */
    @Schema(description = "Owning category ID")
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
    @Size(max = 20, message = "Color value must not exceed 20 characters")
    private String color;

    /**
     * Icon
     */
    @Schema(description = "Icon")
    @Size(max = 50, message = "Icon value must not exceed 50 characters")
    private String icon;
}
```

### 4.2 Create TagUpdateDTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/TagUpdateDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Tag update DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Tag update request")
public class TagUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag ID
     */
    @Schema(description = "Tag ID")
    @NotNull(message = "Tag ID must not be empty")
    private Long id;

    /**
     * Tag name
     */
    @Schema(description = "Tag name")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String tagName;

    /**
     * Tag code
     */
    @Schema(description = "Tag code")
    @Size(max = 50, message = "Tag code must not exceed 50 characters")
    private String tagCode;

    /**
     * Owning category ID
     */
    @Schema(description = "Owning category ID")
    private Long categoryId;

    /**
     * Color
     */
    @Schema(description = "Color")
    @Size(max = 20, message = "Color value must not exceed 20 characters")
    private String color;

    /**
     * Icon
     */
    @Schema(description = "Icon")
    @Size(max = 50, message = "Icon value must not exceed 50 characters")
    private String icon;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
```

### 4.3 Create TagQueryDTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/TagQueryDTO.java`:

```java
package com.knowledge.base.document.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tag query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Tag query request")
public class TagQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    /**
     * Tag name (fuzzy query)
     */
    @Schema(description = "Tag name")
    private String tagName;

    /**
     * Tag type: 0-SYSTEM, 1-USER
     */
    @Schema(description = "Tag type")
    private Integer tagType;

    /**
     * Owning category ID
     */
    @Schema(description = "Owning category ID")
    private Long categoryId;

    /**
     * Status
     */
    @Schema(description = "Status")
    private Integer status;
}
```

### 4.4 Create TagVO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/TagVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tag VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tag information")
public class TagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Tag ID
     */
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
     * Owning category ID
     */
    @Schema(description = "Owning category ID")
    private Long categoryId;

    /**
     * Category name
     */
    @Schema(description = "Category name")
    private String categoryName;

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
     * Status
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
```

---

## V. Data Access Layer

### 5.1 Create the Mapper Interface

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/TagMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * Tag Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

}
```

### 5.2 Create the Mapper XML

Create `kb-document/src/main/resources/mapper/TagMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.TagMapper">

    <!-- Common query result mapping -->
    <resultMap id="BaseResultMap" type="com.knowledge.base.document.entity.Tag">
        <id column="id" property="id" />
        <result column="tag_name" property="tagName" />
        <result column="tag_code" property="tagCode" />
        <result column="category_id" property="categoryId" />
        <result column="tag_type" property="tagType" />
        <result column="color" property="color" />
        <result column="icon" property="icon" />
        <result column="doc_count" property="docCount" />
        <result column="status" property="status" />
        <result column="deleted" property="deleted" />
        <result column="version" property="version" />
        <result column="create_time" property="createTime" />
        <result column="update_time" property="updateTime" />
        <result column="create_by" property="createBy" />
        <result column="update_by" property="updateBy" />
    </resultMap>

    <!-- Common query result columns -->
    <sql id="Base_Column_List">
        id, tag_name, tag_code, category_id, tag_type, color, icon, doc_count, status,
        deleted, version, create_time, update_time, create_by, update_by
    </sql>

    <!-- Query tag list by category ID -->
    <select id="selectByCategoryId" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List" />
        FROM tb_tag
        WHERE category_id = #{categoryId}
        AND deleted = 0
        ORDER BY doc_count DESC
    </select>

    <!-- Query a tag by tag code -->
    <select id="selectByTagCode" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List" />
        FROM tb_tag
        WHERE tag_code = #{tagCode}
        AND deleted = 0
        LIMIT 1
    </select>

    <!-- Query popular tags -->
    <select id="selectHotTags" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List" />
        FROM tb_tag
        WHERE status = 1
        AND deleted = 0
        ORDER BY doc_count DESC
        LIMIT #{limit}
    </select>

    <!-- Update the document count -->
    <update id="updateDocumentCount">
        UPDATE tb_tag
        SET doc_count = #{count}
        WHERE id = #{tagId}
        AND deleted = 0
    </update>

    <!-- Increment the document count -->
    <update id="incrementDocumentCount">
        UPDATE tb_tag
        SET doc_count = doc_count + 1
        WHERE id = #{tagId}
        AND deleted = 0
    </update>

    <!-- Decrement the document count -->
    <update id="decrementDocumentCount">
        UPDATE tb_tag
        SET doc_count = GREATEST(doc_count - 1, 0)
        WHERE id = #{tagId}
        AND deleted = 0
    </update>

    <!-- Fuzzy search of tags -->
    <select id="searchByName" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List" />
        FROM tb_tag
        WHERE tag_name LIKE CONCAT('%', #{keyword}, '%')
        AND status = 1
        AND deleted = 0
        ORDER BY doc_count DESC
    </select>

</mapper>
```

---

## VI. Business Logic Layer

### 6.1 Create the Service Interface

Create `kb-document/src/main/java/com/knowledge/base/document/service/TagService.java`:

```java
package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.entity.Tag;
import com.knowledge.base.document.vo.TagVO;

import java.util.List;

/**
 * Tag Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface TagService extends IService<Tag> {

    /**
     * Create a tag
     *
     * @param dto the creation DTO
     * @return the tag ID
     */
    Long createTag(TagCreateDTO dto);

    /**
     * Update a tag
     *
     * @param dto the update DTO
     * @return whether it succeeded
     */
    Boolean updateTag(TagUpdateDTO dto);

    /**
     * Delete a tag
     *
     * @param tagId the tag ID
     * @return whether it succeeded
     */
    Boolean deleteTag(Long tagId);

    /**
     * Get tag details
     *
     * @param tagId the tag ID
     * @return the tag VO
     */
    TagVO getTagDetail(Long tagId);

    /**
     * Paginated query of tags
     *
     * @param dto the query DTO
     * @return the paginated result
     */
    PageResult<TagVO> pageTags(TagQueryDTO dto);

    /**
     * Get popular tags
     *
     * @param limit the result limit
     * @return the tag list
     */
    List<TagVO> getHotTags(Integer limit);

    /**
     * Get tags by category
     *
     * @param categoryId the category ID
     * @return the tag list
     */
    List<TagVO> getTagsByCategory(Long categoryId);

    /**
     * Batch-create tags
     *
     * @param tagNames the list of tag names
     * @return the list of tag IDs
     */
    List<Long> batchCreateTags(List<String> tagNames);
}
```

### 6.2 Create the Service Implementation Class

Create `kb-document/src/main/java/com/knowledge/base/document/service/impl/TagServiceImpl.java`:

```java
package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.entity.Tag;
import com.knowledge.base.document.mapper.TagMapper;
import com.knowledge.base.document.service.TagService;
import com.knowledge.base.document.vo.TagVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tag Service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(TagCreateDTO dto) {
        log.info("Create tag: tagName={}", dto.getTagName());

        // Check whether the tag name already exists
        Tag existTag = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getTagName, dto.getTagName())
        );
        if (existTag != null) {
            throw new BusinessException("Tag name already exists");
        }

        // Generate the tag code
        String tagCode = StringUtils.hasText(dto.getTagName())
                ? generateTagCode(dto.getTagName())
                : "TAG_" + System.currentTimeMillis();

        // Build the tag entity
        Tag tag = new Tag();
        tag.setId(SnowflakeIdGenerator.getInstance().nextId());
        tag.setTagName(dto.getTagName());
        tag.setTagCode(tagCode);
        tag.setCategoryId(dto.getCategoryId());
        tag.setTagType(dto.getTagType() != null ? dto.getTagType() : 1);
        tag.setColor(dto.getColor());
        tag.setIcon(dto.getIcon());
        tag.setDocCount(0);
        tag.setStatus(1);

        // Save the tag
        int count = tagMapper.insert(tag);
        if (count <= 0) {
            throw new BusinessException("Failed to create tag");
        }

        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTag(TagUpdateDTO dto) {
        log.info("Update tag: tagId={}", dto.getId());

        if (dto.getId() == null) {
            throw new BusinessException("Tag ID must not be empty");
        }

        // Check whether the tag exists
        Tag existTag = tagMapper.selectById(dto.getId());
        if (existTag == null) {
            throw new BusinessException("Tag does not exist");
        }

        // Check whether the tag name is used by another tag
        if (StringUtils.hasText(dto.getTagName())
                && !dto.getTagName().equals(existTag.getTagName())) {
            Tag tag = tagMapper.selectOne(
                    new LambdaQueryWrapper<Tag>()
                            .eq(Tag::getTagName, dto.getTagName())
            );
            if (tag != null && !tag.getId().equals(dto.getId())) {
                throw new BusinessException("Tag name is already in use");
            }
        }

        // Build the update entity
        Tag tag = new Tag();
        tag.setId(dto.getId());
        if (StringUtils.hasText(dto.getTagName())) {
            tag.setTagName(dto.getTagName());
        }
        if (dto.getCategoryId() != null) {
            tag.setCategoryId(dto.getCategoryId());
        }
        if (dto.getColor() != null) {
            tag.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) {
            tag.setIcon(dto.getIcon());
        }
        if (dto.getStatus() != null) {
            tag.setStatus(dto.getStatus());
        }

        int count = tagMapper.updateById(tag);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTag(Long tagId) {
        log.info("Delete tag: tagId={}", tagId);

        if (tagId == null) {
            throw new BusinessException("Tag ID must not be empty");
        }

        // Check whether the tag exists
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("Tag does not exist");
        }

        // Check whether it has associated documents
        if (tag.getDocCount() != null && tag.getDocCount() > 0) {
            throw new BusinessException("This tag has documents attached and cannot be deleted");
        }

        // Delete the tag
        int count = tagMapper.deleteById(tagId);
        return count > 0;
    }

    @Override
    public TagVO getTagDetail(Long tagId) {
        if (tagId == null) {
            throw new BusinessException("Tag ID must not be empty");
        }

        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("Tag does not exist");
        }

        return convertToVO(tag);
    }

    @Override
    public PageResult<TagVO> pageTags(TagQueryDTO dto) {
        // Build the query conditions
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getTagName())) {
            wrapper.like(Tag::getTagName, dto.getTagName())
                    .or()
                    .like(Tag::getTagCode, dto.getTagName());
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(Tag::getCategoryId, dto.getCategoryId());
        }
        if (dto.getTagType() != null) {
            wrapper.eq(Tag::getTagType, dto.getTagType());
        }
        wrapper.eq(Tag::getStatus, 1);

        // Paginated query
        Page<Tag> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<Tag> tagPage = tagMapper.selectPage(page, wrapper);

        // Convert to VO
        IPage<TagVO> voPage = tagPage.convert(this::convertToVO);

        return PageResult.<TagVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    @Override
    public List<TagVO> getHotTags(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getStatus, 1)
                        .orderByDesc(Tag::getDocCount)
                        .last("LIMIT " + limit)
        );

        return tags.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getTagsByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("Category ID must not be empty");
        }

        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getCategoryId, categoryId)
                        .eq(Tag::getStatus, 1)
                        .orderByDesc(Tag::getDocCount)
        );

        return tags.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateTags(List<String> tagNames) {
        log.info("Batch-create tags: tagCount={}", tagNames.size());

        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = new ArrayList<>();

        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }

            // Check whether the tag already exists
            Tag existTag = tagMapper.selectOne(
                    new LambdaQueryWrapper<Tag>()
                            .eq(Tag::getTagName, tagName.trim())
            );

            Long tagId;
            if (existTag != null) {
                tagId = existTag.getId();
            } else {
                // Create a new tag
                TagCreateDTO dto = new TagCreateDTO();
                dto.setTagName(tagName.trim());
                dto.setTagType("USER");

                tagId = createTag(dto);
            }

            tagIds.add(tagId);
        }

        return tagIds;
    }

    /**
     * Convert to VO
     *
     * @param tag the tag entity
     * @return the tag VO
     */
    private TagVO convertToVO(Tag tag) {
        return TagVO.builder()
                .id(tag.getId())
                .tagName(tag.getTagName())
                .tagCode(tag.getTagCode())
                .categoryId(tag.getCategoryId())
                .tagType(tag.getTagType())
                .color(tag.getColor())
                .icon(tag.getIcon())
                .docCount(tag.getDocCount() != null ? tag.getDocCount() : 0)
                .status(tag.getStatus())
                .createdAt(tag.getCreateTime())
                .build();
    }

    /**
     * Generate a tag code
     *
     * @param tagName the tag name
     * @return the tag code
     */
    private String generateTagCode(String tagName) {
        // Simple code generation logic
        return "TAG_" + tagName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "") + "_" + System.currentTimeMillis();
    }
}
```

### 6.3 Core Method Notes

#### 6.3.1 Batch-Creating Tags

Batch tag creation implements the logic of "reuse if it exists, create if it doesn't":

```java
@Override
@Transactional(rollbackFor = Exception.class)
public List<Long> batchCreateTags(List<String> tagNames) {
    List<Long> tagIds = new ArrayList<>();

    for (String tagName : tagNames) {
        // Check whether the tag already exists
        Tag existTag = tagMapper.selectOne(
            new LambdaQueryWrapper<Tag>()
                .eq(Tag::getTagName, tagName.trim())
        );

        if (existTag != null) {
            tagIds.add(existTag.getId());
        } else {
            // Create a new tag
            TagCreateDTO dto = new TagCreateDTO();
            dto.setTagName(tagName.trim());
            dto.setTagType(1); // 1-USER type
            Long tagId = createTag(dto);
            tagIds.add(tagId);
        }
    }

    return tagIds;
}
```

This method is very useful when editing documents: after a user enters tags, the system automatically creates the ones that don't yet exist.

---

## VII. Controller Layer

### 7.1 Create TagController

Create `kb-document/src/main/java/com/knowledge/base/document/controller/TagController.java`:

```java
package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.service.TagService;
import com.knowledge.base.document.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tag management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tag Management", description = "Tag management endpoints")
public class TagController {

    @Resource
    private TagService tagService;

    /**
     * Create a tag
     */
    @PostMapping
    @Operation(summary = "Create tag", description = "Create a new tag")
    public Result<Long> createTag(@Valid @RequestBody TagCreateDTO dto) {
        log.info("Create tag request: tagName={}", dto.getTagName());

        Long tagId = tagService.createTag(dto);
        return Result.success("Tag created successfully", tagId);
    }

    /**
     * Update a tag
     */
    @PutMapping
    @Operation(summary = "Update tag", description = "Update tag information")
    public Result<Boolean> updateTag(@Valid @RequestBody TagUpdateDTO dto) {
        log.info("Update tag request: tagId={}", dto.getId());

        Boolean result = tagService.updateTag(dto);
        return Result.success("Tag updated successfully", result);
    }

    /**
     * Delete a tag
     */
    @DeleteMapping("/{tagId}")
    @Operation(summary = "Delete tag", description = "Delete the specified tag")
    public Result<Boolean> deleteTag(
        @Parameter(description = "Tag ID", required = true)
        @PathVariable Long tagId) {
        log.info("Delete tag request: tagId={}", tagId);

        Boolean result = tagService.deleteTag(tagId);
        return Result.success("Tag deleted successfully", result);
    }

    /**
     * Get tag details
     */
    @GetMapping("/{tagId}")
    @Operation(summary = "Get tag details", description = "Get tag details by ID")
    public Result<TagVO> getTagDetail(
        @Parameter(description = "Tag ID", required = true)
        @PathVariable Long tagId) {
        log.info("Get tag details request: tagId={}", tagId);

        TagVO tagVO = tagService.getTagDetail(tagId);
        return Result.success(tagVO);
    }

    /**
     * Paginated query of tags
     */
    @PostMapping("/page")
    @Operation(summary = "Paginated query of tags", description = "Paginated query of the tag list")
    public Result<PageResult<TagVO>> pageTags(@RequestBody TagQueryDTO dto) {
        log.info("Paginated tag query request: current={}, size={}", dto.getCurrent(), dto.getSize());

        PageResult<TagVO> pageResult = tagService.pageTags(dto);
        return Result.success(pageResult);
    }

    /**
     * Get popular tags
     */
    @GetMapping("/hot")
    @Operation(summary = "Get popular tags", description = "Get the most-used tags")
    public Result<List<TagVO>> getHotTags(
        @Parameter(description = "Result limit") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("Get popular tags request: limit={}", limit);

        List<TagVO> hotTags = tagService.getHotTags(limit);
        return Result.success(hotTags);
    }

    /**
     * Get tags by category
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get tags by category", description = "Get the tags under the specified category")
    public Result<List<TagVO>> getTagsByCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable Long categoryId) {
        log.info("Get tags by category request: categoryId={}", categoryId);

        List<TagVO> tags = tagService.getTagsByCategory(categoryId);
        return Result.success(tags);
    }

    /**
     * Search tags
     */
    @GetMapping("/search")
    @Operation(summary = "Search tags", description = "Search tags by keyword")
    public Result<List<TagVO>> searchTags(
        @Parameter(description = "Search keyword", required = true)
        @RequestParam String keyword) {
        log.info("Search tags request: keyword={}", keyword);

        TagQueryDTO dto = new TagQueryDTO();
        dto.setTagName(keyword);
        dto.setCurrent(1L);
        dto.setSize(20L);

        PageResult<TagVO> pageResult = tagService.pageTags(dto);
        return Result.success(pageResult.getRecords());
    }
}
```

### 7.2 RESTful API Design

| Method | Path | Description | Parameters |
|------|------|------|------|
| POST | /api/tags | Create a tag | TagCreateDTO |
| PUT | /api/tags | Update a tag | TagUpdateDTO |
| DELETE | /api/tags/{id} | Delete a tag | tagId |
| GET | /api/tags/{id} | Query a tag | tagId |
| POST | /api/tags/page | Paginated query of tags | TagQueryDTO |
| GET | /api/tags/hot | Get popular tags | limit |
| GET | /api/tags/category/{id} | Get tags by category | categoryId |
| GET | /api/tags/search | Search tags | keyword |

---

## VIII. Testing and Verification

### 8.1 Initialize Test Data

Run the following SQL to insert test data:

```sql
-- Insert system tags (tag_type = 0)
INSERT INTO tb_tag (id, tag_name, tag_code, category_id, tag_type, color, icon, doc_count, status) VALUES
(1000000000000000001, 'Pinned', 'TAG_TOP', NULL, 0, '#ff4d4f', '⭐', 0, 1),
(1000000000000000002, 'Recommended', 'TAG_RECOMMEND', NULL, 0, '#ffec3d', '🔥', 0, 1),
(1000000000000000003, 'Featured', 'TAG_FEATURED', NULL, 0, '#ffd666', '✨', 0, 1),
(1000000000000000004, 'Original', 'TAG_ORIGINAL', NULL, 0, '#95de64', '📝', 5, 1),
(1000000000000000005, 'Translation', 'TAG_TRANSLATE', NULL, 0, '#b37feb', '🌐', 2, 1);

-- Insert user tags (tag_type = 1)
INSERT INTO tb_tag (id, tag_name, tag_code, category_id, tag_type, color, icon, doc_count, status) VALUES
(1000000000000000006, 'Java', 'TAG_JAVA', 1000000000000000004, 1, '#f759ab', '☕', 10, 1),
(1000000000000000007, 'Spring Boot', 'TAG_SPRING_BOOT', 1000000000000000004, 1, '#6dd400', '🍃', 8, 1),
(1000000000000000008, 'Vue.js', 'TAG_VUE', 1000000000000000005, 1, '#42b883', '💚', 6, 1),
(1000000000000000009, 'React', 'TAG_REACT', 1000000000000000005, 1, '#61dafb', '⚛️', 5, 1),
(1000000000000000010, 'Python', 'TAG_PYTHON', 1000000000000000004, 1, '#ffd43b', '🐍', 7, 1);
```

### 8.2 Test Tag CRUD

#### Create a Tag

```bash
curl -X POST http://localhost:8082/api/document/api/tags \
  -H "Content-Type: application/json" \
  -d '{
    "tagName": "MyBatis",
    "categoryId": 1000000000000000004,
    "tagType": 1,
    "color": "#e14329",
    "icon": "🍂"
  }'
```

#### Update a Tag

```bash
curl -X PUT http://localhost:8082/api/document/api/tags \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1000000000000000006,
    "color": "#1890ff"
  }'
```

#### Query Tag Details

```bash
curl http://localhost:8082/api/document/api/tags/1000000000000000006
```

#### Paginated Query of Tags

```bash
curl -X POST http://localhost:8082/api/document/api/tags/page \
  -H "Content-Type: application/json" \
  -d '{
    "current": 1,
    "size": 10,
    "tagType": 1
  }'
```

#### Get Popular Tags

```bash
curl "http://localhost:8082/api/document/api/tags/hot?limit=5"
```

#### Get Tags by Category

```bash
curl "http://localhost:8082/api/document/api/tags/category/1000000000000000004"
```

#### Search Tags

```bash
curl "http://localhost:8082/api/document/api/tags/search?keyword=Java"
```

#### Delete a Tag

```bash
curl -X DELETE "http://localhost:8082/api/document/api/tags/1000000000000000010"
```

### 8.3 Verify the Data Structure

Example response from the popular tags endpoint:

```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": [
    {
      "id": 1000000000000000006,
      "tagName": "Java",
      "tagCode": "TAG_JAVA",
      "categoryId": 1000000000000000004,
      "tagType": 1,
      "color": "#f759ab",
      "icon": "☕",
      "docCount": 10,
      "status": 1,
      "createdAt": "2026-05-13T10:30:00"
    },
    {
      "id": 1000000000000000007,
      "tagName": "Spring Boot",
      "tagCode": "TAG_SPRING_BOOT",
      "categoryId": 1000000000000000004,
      "tagType": 1,
      "color": "#6dd400",
      "icon": "🍃",
      "docCount": 8,
      "status": 1,
      "createdAt": "2026-05-13T10:30:00"
    }
  ]
}
```

---

## IX. Advanced Features

### 9.1 Tag Color Management

Provide preset colors for users to choose from:

```java
/**
 * Tag color Controller
 */
@RestController
@RequestMapping("/api/tags")
public class TagColorController {

    /**
     * Get the recommended colors
     */
    @GetMapping("/colors")
    @Operation(summary = "Get recommended colors", description = "Get the list of recommended tag colors")
    public Result<List<String>> getRecommendedColors() {
        List<String> colors = Arrays.asList(
            "#f759ab", "#722ed1", "#2f54eb", "#1890ff", "#13c2c2",
            "#52c41a", "#aad676", "#95de64", "#ffd666", "#ffec3d",
            "#ff9c6e", "#ff4d4f", "#f759ab", "#b37feb", "#722ed1"
        );
        return Result.success(colors);
    }
}
```

### 9.2 Tag Cloud Feature

Implement a tag cloud display:

```java
/**
 * Get the tag cloud
 */
@Override
public List<TagVO> getTagCloud() {
    List<Tag> tags = tagMapper.selectList(
        new LambdaQueryWrapper<Tag>()
            .eq(Tag::getStatus, 1)
            .eq(Tag::getDocCount, 0)
            .orderByAsc(Tag::getDocCount)
    );

    // Rank by document count
    int maxDocCount = tags.stream()
        .mapToInt(tag -> tag.getDocCount() != null ? tag.getDocCount() : 0)
        .max()
        .orElse(1);

    return tags.stream()
        .map(tag -> {
            TagVO vo = convertToVO(tag);
            // Compute the tag size level 1-5
            int level = (int) Math.ceil((double) tag.getDocCount() / maxDocCount * 5);
            vo.setLevel(level);
            return vo;
        })
        .collect(Collectors.toList());
}
```

### 9.3 Intelligent Tag Recommendations

Recommend tags based on document content:

```java
/**
 * Intelligently recommend tags
 */
public List<TagVO> recommendTags(String content, Long categoryId) {
    // 1. Get popular tags in the same category
    List<Tag> categoryTags = tagMapper.selectList(
        new LambdaQueryWrapper<Tag>()
            .eq(Tag::getCategoryId, categoryId)
            .eq(Tag::getStatus, 1)
            .orderByDesc(Tag::getDocCount)
            .last("LIMIT 5")
    );

    // 2. Extract keywords and match tags
    List<Tag> matchedTags = tagMapper.selectList(
        new LambdaQueryWrapper<Tag>()
            .eq(Tag::getStatus, 1)
            .and(wrapper -> {
                for (String keyword : extractKeywords(content)) {
                    wrapper.or().like(Tag::getTagName, keyword);
                }
            })
            .last("LIMIT 5")
    );

    // 3. Merge and deduplicate
    Set<Long> tagIds = new HashSet<>();
    List<TagVO> recommendations = new ArrayList<>();

    for (Tag tag : categoryTags) {
        if (tagIds.add(tag.getId())) {
            recommendations.add(convertToVO(tag));
        }
    }

    for (Tag tag : matchedTags) {
        if (tagIds.add(tag.getId())) {
            recommendations.add(convertToVO(tag));
        }
    }

    return recommendations;
}

/**
 * Simple keyword extraction
 */
private List<String> extractKeywords(String content) {
    // Simple implementation: extract common programming languages, frameworks, etc.
    return Arrays.asList("Java", "Python", "JavaScript", "Vue", "React",
        "Spring", "MyBatis", "MySQL", "Redis", "Docker");
}
```

### 9.4 Tag Usage Statistics

Track tag usage trends:

```java
/**
 * Get tag usage statistics
 */
public Map<String, Object> getTagStatistics() {
    // Total tag count
    Long totalTags = tagMapper.selectCount(
        new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, 1)
    );

    // Number of tags created today
    Long todayTags = tagMapper.selectCount(
        new LambdaQueryWrapper<Tag>()
            .eq(Tag::getStatus, 1)
            .ge(Tag::getCreateTime, LocalDate.now())
    );

    // Average tags per document
    Long totalDocuments = documentMapper.selectCount(/*...*/);
    Long totalTagUsages = documentTagMapper.selectCount(/*...*/);
    double avgTagsPerDoc = (double) totalTagUsages / totalDocuments;

    // Tag type distribution
    Map<String, Long> typeDistribution = tagMapper.selectList(
        new LambdaQueryWrapper<Tag>()
            .select(Tag::getTagType)
            .eq(Tag::getStatus, 1)
    ).stream()
        .collect(Collectors.groupingBy(Tag::getTagType, Collectors.counting()));

    Map<String, Object> statistics = new HashMap<>();
    statistics.put("totalTags", totalTags);
    statistics.put("todayTags", todayTags);
    statistics.put("avgTagsPerDoc", avgTagsPerDoc);
    statistics.put("typeDistribution", typeDistribution);

    return statistics;
}
```

---

## X. Frequently Asked Questions

### 10.1 Tag Name Case Sensitivity

**Problem**: A user entering "java" and "Java" creates two separate tags.

**Solution**: normalize to uppercase or lowercase before storing

```java
@Override
public Long createTag(TagCreateDTO dto) {
    // Normalize the tag name to lowercase
    String normalizedName = dto.getTagName().trim().toLowerCase();

    Tag existTag = tagMapper.selectOne(
        new LambdaQueryWrapper<Tag>()
            .eq(Tag::getTagName, normalizedName)
    );
    // ...
}
```

### 10.2 Tag Count Limits

**Problem**: too many tags on a single document can hurt performance.

**Solution**: limit the maximum number of tags per document

```java
private static final int MAX_TAGS_PER_DOCUMENT = 10;

public void addTagsToDocument(Long documentId, List<Long> tagIds) {
    if (tagIds.size() > MAX_TAGS_PER_DOCUMENT) {
        throw new BusinessException("A single document can have at most " + MAX_TAGS_PER_DOCUMENT + " tags");
    }
    // ...
}
```

### 10.3 Tag Color Conventions

**Problem**: inconsistent color choices by users hurt the UI's visual appeal.

**Solution**: offer preset colors along with a custom color option

```java
// Frontend uses preset colors
const PRESET_COLORS = [
    { value: '#f759ab', label: 'Pink' },
    { value: '#722ed1', label: 'Purple' },
    { value: '#2f54eb', label: 'Blue' },
    { value: '#1890ff', label: 'Sky Blue' },
    { value: '#52c41a', label: 'Green' },
    { value: '#ffec3d', label: 'Yellow' },
    { value: '#ff4d4f', label: 'Red' },
    { value: '#999999', label: 'Gray' }
];
```

### 10.4 Tag Merging Feature

**Problem**: users create similar tags that need to be merged.

**Solution**: provide a tag merging feature

```java
/**
 * Merge tags
 */
@Transactional(rollbackFor = Exception.class)
public Boolean mergeTags(Long sourceTagId, Long targetTagId) {
    // 1. Check whether the tags exist
    Tag sourceTag = tagMapper.selectById(sourceTagId);
    Tag targetTag = tagMapper.selectById(targetTagId);

    if (sourceTag == null || targetTag == null) {
        throw new BusinessException("Tag does not exist");
    }

    // 2. Update document-tag associations
    documentTagMapper.update(null,
        new LambdaUpdateWrapper<DocumentTag>()
            .eq(DocumentTag::getTagId, sourceTagId)
            .set(DocumentTag::getTagId, targetTagId)
    );

    // 3. Update the target tag's document count
    targetTag.setDocCount(targetTag.getDocCount() + sourceTag.getDocCount());
    tagMapper.updateById(targetTag);

    // 4. Delete the source tag
    tagMapper.deleteById(sourceTagId);

    return true;
}
```

---

## XI. Integrating with the Document Module

### 11.1 Adding Tags to a Document

Add the tag association in DocumentService:

```java
/**
 * Add tags to a document
 */
@Transactional(rollbackFor = Exception.class)
public Boolean addTagsToDocument(Long documentId, List<Long> tagIds) {
    // 1. Verify the document exists
    Document document = documentMapper.selectById(documentId);
    if (document == null) {
        throw new BusinessException("Document does not exist");
    }

    // 2. Delete the old tag associations
    documentTagMapper.delete(
        new LambdaQueryWrapper<DocumentTag>()
            .eq(DocumentTag::getDocumentId, documentId)
    );

    // 3. Create the new tag associations
    for (Long tagId : tagIds) {
        DocumentTag documentTag = new DocumentTag();
        documentTag.setId(SnowflakeIdGenerator.getInstance().nextId());
        documentTag.setDocumentId(documentId);
        documentTag.setTagId(tagId);
        documentTag.setCreateTime(LocalDateTime.now());
        documentTagMapper.insert(documentTag);

        // Increment the tag's document count
        tagMapper.incrementDocumentCount(tagId);
    }

    // 4. Update the document's tags field (denormalized)
    TagExample tagExample = new TagExample();
    tagExample.createCriteria().andIdIn(tagIds);
    List<Tag> tags = tagMapper.selectByExample(tagExample);
    String tagNames = tags.stream()
        .map(Tag::getTagName)
        .collect(Collectors.joining(","));

    Document updateDoc = new Document();
    updateDoc.setId(documentId);
    updateDoc.setTags(tagNames);
    documentMapper.updateById(updateDoc);

    return true;
}
```

### 11.2 Updating Tag Counts When a Document Is Deleted

```java
/**
 * Handle tags before removing a document
 */
@PreRemove
public void beforeRemove(Document document) {
    // Get all tags for the document
    List<Long> tagIds = documentTagMapper.selectList(
        new LambdaQueryWrapper<DocumentTag>()
            .eq(DocumentTag::getDocumentId, document.getId())
    ).stream()
        .map(DocumentTag::getTagId)
        .collect(Collectors.toList());

    // Delete the tag associations
    documentTagMapper.delete(
        new LambdaQueryWrapper<DocumentTag>()
            .eq(DocumentTag::getDocumentId, document.getId())
    );

    // Decrement the document count of each tag
    for (Long tagId : tagIds) {
        tagMapper.decrementDocumentCount(tagId);
    }
}
```

---

## XII. Summary

This article described in detail the complete implementation of the tag management feature, covering the following core content:

### Completed Features

| Feature | Implementation | Description |
|------|---------|------|
| Tag CRUD | Service + Controller | Complete create/read/update/delete |
| Popular tags | Sorted by docCount | Usage frequency statistics |
| Category association | Filtered by categoryId | Tag category management |
| Batch creation | Duplicate checking | Reuse if it exists, create if it doesn't |
| Tag search | Fuzzy query | Supports autocomplete |

### Technical Highlights

1. **Tag type distinction**: SYSTEM and USER types
2. **Unique tag code**: enforced via a unique index
3. **Document count statistics**: denormalized docCount field
4. **Batch creation optimization**: checks whether the tag already exists
5. **Color and icon**: provides a visual representation

### Design Highlights

1. **Flexible category system**: tags associated with categories for easier management
2. **Intelligent batch operations**: automatic deduplication during batch creation
3. **Rich query options**: supports multi-dimensional filtering and search
4. **Full extensibility**: supports extension attributes like tag color and icon

### Future Improvements

1. Add a tag merging feature
2. Implement a tag cloud display
3. Support tag weight calculation
4. Implement intelligent tag recommendations
5. Add tag usage statistics and trend analysis

Through this article, you should now be able to grasp:
- How to design a tag management system
- Advanced MyBatis Plus query techniques
- How to handle batch operations
- The design of tag-document associations
- RESTful API design conventions

Happy building!
