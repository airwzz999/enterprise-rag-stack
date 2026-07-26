# Adding the Document Version Feature

## I. Overview

Document version management is one of the core features of the knowledge base system. It records the modification history of every document, and supports version rollback, version comparison, and version restoration. This article describes in detail how to implement a complete document version management feature in the kb-document service.

### 1.1 Feature Positioning

Version management, as an important tool for document control, provides the following core features:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Version creation | Automatically records document modification history | A version is automatically created each time the document is saved |
| Version list | View all versions of a document | Browsing version history |
| Version detail | View the complete content of a specified version | Viewing version content |
| Version restore | Restore to a specified historical version | Rolling back content |
| Version comparison | Compare the differences between two versions | Viewing changes |

### 1.2 Version Management Strategy

**Automatic version creation strategy:**
- A new version is automatically created every time the document is saved
- The most recent N versions are retained; older versions can be archived

**Version numbering rules:**
- Version numbers start at 1 and increment
- Automatically incremented on every modification
- Version numbers are globally unique

### 1.3 Data Model

```
Document
    ├── Version 1 (DocumentVersion) - initial version
    ├── Version 2 (DocumentVersion) - first modification
    ├── Version 3 (DocumentVersion) - second modification
    └── ...
```

---

## II. Database Design

### 2.1 Document Version Table Structure

```sql
CREATE TABLE `tb_document_version` (
  `id` BIGINT NOT NULL COMMENT 'Version ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `version` INT NOT NULL COMMENT 'Version number',
  `title` VARCHAR(200) NOT NULL COMMENT 'Document title',
  `content` LONGTEXT NOT NULL COMMENT 'Document content',
  `summary` TEXT DEFAULT NULL COMMENT 'Document summary',
  `change_description` VARCHAR(500) DEFAULT NULL COMMENT 'Version change description',
  `change_size` BIGINT DEFAULT NULL COMMENT 'Change size (bytes)',
  `operator_id` BIGINT NOT NULL COMMENT 'Operator ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'Operator name',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version` (`document_id`, `version`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document version table';
```

### 2.2 Field Descriptions

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Version ID | Yes | Auto-increment |
| document_id | BIGINT | Document ID | Yes | - |
| version | INT | Version number | Yes | - |
| title | VARCHAR(200) | Document title | Yes | - |
| content | LONGTEXT | Document content | Yes | - |
| summary | TEXT | Document summary | No | - |
| change_description | VARCHAR(500) | Version change description | No | - |
| change_size | BIGINT | Change size (bytes) | No | - |
| operator_id | BIGINT | Operator ID | Yes | - |
| operator_name | VARCHAR(50) | Operator name | No | - |
| created_at | DATETIME | Creation time | Yes | CURRENT_TIMESTAMP |

### 2.3 Index Design

- **Primary key index**: `id` - uniquely identifies the primary key
- **Unique index**: `uk_doc_version` - ensures the version number is unique per document
- **Regular index**: `idx_document_id` - speeds up querying versions by document
- **Regular index**: `idx_operator_id` - speeds up querying by operator
- **Regular index**: `idx_created_at` - speeds up sorting by time

---

## III. Entity Class Design

### 3.1 Create the DocumentVersion Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/DocumentVersion.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document version entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("tb_document_version")
@Schema(description = "Document version entity")
public class DocumentVersion {

    /**
     * Version ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Version ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Version number
     */
    @Schema(description = "Version number")
    private Integer version;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Document content
     */
    @Schema(description = "Document content")
    private String content;

    /**
     * Document summary
     */
    @Schema(description = "Document summary")
    private String summary;

    /**
     * Version change description
     */
    @Schema(description = "Version change description")
    private String changeDescription;

    /**
     * Change size (bytes)
     */
    @Schema(description = "Change size")
    private Long changeSize;

    /**
     * Operator ID
     */
    @Schema(description = "Operator ID")
    private Long operatorId;

    /**
     * Operator name
     */
    @Schema(description = "Operator name")
    private String operatorName;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
```

### 3.2 Design Notes

1. **Version number uniqueness**: uses a composite unique index on `document_id` and `version`
2. **Change size**: records the size of the content difference from the previous version
3. **Operator information**: the operator name is denormalized to avoid frequent join queries
4. **Large field storage**: `content` uses the LONGTEXT type to support storing large documents

---

## IV. DTO and VO Design

### 4.1 Create DocumentVersionRestoreDTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/DocumentVersionRestoreDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document version restore DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document version restore parameters")
public class DocumentVersionRestoreDTO {

    /**
     * Version ID
     */
    @Schema(description = "Version ID")
    @NotNull(message = "Version ID must not be empty")
    private Long versionId;

    /**
     * Reason for restoring
     */
    @Schema(description = "Reason for restoring")
    private String reason;
}
```

### 4.2 Create DocumentVersionVO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/DocumentVersionVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document version VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document version information")
public class DocumentVersionVO {

    /**
     * Version ID
     */
    @Schema(description = "Version ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Version number
     */
    @Schema(description = "Version number")
    private Integer version;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Version change description
     */
    @Schema(description = "Version change description")
    private String changeDescription;

    /**
     * Change size (bytes)
     */
    @Schema(description = "Change size")
    private Long changeSize;

    /**
     * Operator ID
     */
    @Schema(description = "Operator ID")
    private Long operatorId;

    /**
     * Operator name
     */
    @Schema(description = "Operator name")
    private String operatorName;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
```

### 4.3 Design Notes

**Differences between DTO and VO:**

| Type | Purpose | Characteristics |
|------|------|------|
| RestoreDTO | Receives restore requests | Concise, only contains the necessary parameters |
| VO | Returns version information | Includes all display fields |

**Data safety considerations:**
- The VO does not return the full `content`, to avoid an overly large response
- The full content is only returned when viewing version details

---

## V. Data Access Layer

### 5.1 Create the Mapper Interface

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/DocumentVersionMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * Document version Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

}
```

### 5.2 Create the Mapper XML

Create `kb-document/src/main/resources/mapper/DocumentVersionMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.DocumentVersionMapper">

    <!-- Common query result mapping -->
    <resultMap id="BaseResultMap" type="com.knowledge.base.document.entity.DocumentVersion">
        <id column="id" property="id" />
        <result column="document_id" property="documentId" />
        <result column="version" property="version" />
        <result column="title" property="title" />
        <result column="content" property="content" />
        <result column="summary" property="summary" />
        <result column="change_description" property="changeDescription" />
        <result column="change_size" property="changeSize" />
        <result column="operator_id" property="operatorId" />
        <result column="operator_name" property="operatorName" />
        <result column="created_at" property="createdAt" />
    </resultMap>

    <!-- Common query result columns -->
    <sql id="Base_Column_List">
        id, document_id, version, title, content, summary, change_description, change_size,
        operator_id, operator_name, created_at
    </sql>

    <!-- Query the version list by document ID -->
    <select id="selectByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE document_id = #{documentId}
        ORDER BY version DESC
    </select>

    <!-- Query a version by document ID and version number -->
    <select id="selectByDocumentIdAndVersion" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE document_id = #{documentId}
        AND version = #{version}
        LIMIT 1
    </select>

    <!-- Query the latest version of a document -->
    <select id="selectLatestByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE document_id = #{documentId}
        ORDER BY version DESC
        LIMIT 1
    </select>

    <!-- Query the first version of a document -->
    <select id="selectFirstByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE document_id = #{documentId}
        ORDER BY version ASC
        LIMIT 1
    </select>

    <!-- Get the next version number for a document -->
    <select id="getNextVersionNumber" resultType="java.lang.Integer">
        SELECT COALESCE(MAX(version), 0) + 1
        FROM tb_document_version
        WHERE document_id = #{documentId}
    </select>

    <!-- Count the number of versions for a document -->
    <select id="countByDocumentId" resultType="java.lang.Long">
        SELECT COUNT(*)
        FROM tb_document_version
        WHERE document_id = #{documentId}
    </select>

    <!-- Query the version list by operator ID -->
    <select id="selectByOperatorId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE operator_id = #{operatorId}
        ORDER BY created_at DESC
    </select>

    <!-- Compare two versions -->
    <select id="compareVersions" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_version
        WHERE document_id = #{documentId}
        AND version IN (#{version1}, #{version2})
        ORDER BY version ASC
    </select>

</mapper>
```

### 5.3 Mapper Method Notes

| Method | Description | Use Case |
|------|------|---------|
| selectByDocumentId | Query all versions of a document | Displaying the version list |
| selectByDocumentIdAndVersion | Query a specified version | Viewing version details |
| selectLatestByDocumentId | Query the latest version | Getting the latest content |
| selectFirstByDocumentId | Query the first version | Viewing the initial version |
| getNextVersionNumber | Get the next version number | When creating a new version |
| countByDocumentId | Count the number of versions | Version count statistics |
| selectByOperatorId | Query versions by operator | Querying operation history |
| compareVersions | Compare two versions | Version comparison feature |

---

## VI. Business Logic Layer

### 6.1 Create the Service Interface

Create `kb-document/src/main/java/com/knowledge/base/document/service/DocumentVersionService.java`:

```java
package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.vo.DocumentVersionVO;

import java.util.List;

/**
 * Document version management service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentVersionService {

    /**
     * Create a document version
     *
     * @param documentId    the document ID
     * @param changeDescription the change description
     * @param userId        the user ID
     * @return whether it succeeded
     */
    boolean createVersion(Long documentId, String changeDescription, Long userId);

    /**
     * Get the document version list
     *
     * @param documentId the document ID
     * @param current    the current page
     * @param size       the page size
     * @return the version list
     */
    IPage<DocumentVersionVO> getVersionList(Long documentId, Long current, Long size);

    /**
     * Get version details
     *
     * @param versionId the version ID
     * @return the version details
     */
    DocumentVersionVO getVersionDetail(Long versionId);

    /**
     * Restore a version
     *
     * @param documentId the document ID
     * @param restoreDTO the restore parameters
     * @param userId     the user ID
     * @return whether it succeeded
     */
    boolean restoreVersion(Long documentId, DocumentVersionRestoreDTO restoreDTO, Long userId);

    /**
     * Compare the differences between versions
     *
     * @param versionId1 version ID 1
     * @param versionId2 version ID 2
     * @return the diff content
     */
    String compareVersions(Long versionId1, Long versionId2);

    /**
     * Delete a version
     *
     * @param versionId the version ID
     * @param userId    the user ID
     * @return whether it succeeded
     */
    boolean deleteVersion(Long versionId, Long userId);
}
```

### 6.2 Create the Service Implementation Class

Create `kb-document/src/main/java/com/knowledge/base/document/service/impl/DocumentVersionServiceImpl.java`:

```java
package com.knowledge.base.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.DocumentVersion;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.DocumentVersionMapper;
import com.knowledge.base.document.service.DocumentVersionService;
import com.knowledge.base.document.vo.DocumentVersionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document version Service implementation class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; implements the
 * business logic related to document versions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentVersionServiceImpl implements DocumentVersionService {

    @Resource
    private DocumentVersionMapper documentVersionMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createVersion(Long documentId, String changeDescription, Long userId) {
        log.info("Create document version: documentId={}, userId={}", documentId, userId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Get the document information
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Get the current maximum version number
        Long currentVersionCount = documentVersionMapper.selectCount(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
        );
        Integer currentVersion = currentVersionCount.intValue();

        // Get the previous version to compute the change size
        Long changeSize = 0L;
        DocumentVersion lastVersion = documentVersionMapper.selectOne(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
                        .orderByDesc(DocumentVersion::getVersion)
                        .last("LIMIT 1")
        );
        if (lastVersion != null && lastVersion.getContent() != null) {
            int oldSize = lastVersion.getContent().length();
            int newSize = document.getContent() != null ? document.getContent().length() : 0;
            changeSize = (long) newSize - oldSize;
        }

        // TODO: get the operator information
        String operatorName = "System User";

        // Create the version record
        DocumentVersion version = new DocumentVersion();
        version.setId(SnowflakeIdGenerator.getInstance().nextId());
        version.setDocumentId(documentId);
        version.setVersion(currentVersion + 1);
        version.setTitle(document.getTitle());
        version.setContent(document.getContent());
        version.setSummary(document.getSummary());
        version.setChangeDescription(changeDescription);
        version.setChangeSize(changeSize);
        version.setOperatorId(userId);
        version.setOperatorName(operatorName);
        version.setCreatedAt(LocalDateTime.now());

        int count = documentVersionMapper.insert(version);
        return count > 0;
    }

    @Override
    public IPage<DocumentVersionVO> getVersionList(Long documentId, Long current, Long size) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Paginated query of the version list
        Page<DocumentVersion> page = new Page<>(current, size);
        IPage<DocumentVersion> versionPage = documentVersionMapper.selectPage(
                page,
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
                        .orderByDesc(DocumentVersion::getVersion)
        );

        // Convert to VO
        return versionPage.convert(this::convertToVO);
    }

    @Override
    public DocumentVersionVO getVersionDetail(Long versionId) {
        if (versionId == null) {
            throw new BusinessException("Version ID must not be empty");
        }

        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        return convertToVO(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreVersion(Long documentId, DocumentVersionRestoreDTO restoreDTO, Long userId) {
        log.info("Restore document version: documentId={}, versionId={}, userId={}", documentId, restoreDTO.getVersionId(), userId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Check whether the version exists
        DocumentVersion version = documentVersionMapper.selectById(restoreDTO.getVersionId());
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        // Check whether the version belongs to this document
        if (!version.getDocumentId().equals(documentId)) {
            throw new BusinessException("The version does not belong to this document");
        }

        // Create a backup of the current version (before restoring)
        createVersion(documentId, "Automatic backup before restore", userId);

        // Restore the document content
        document.setTitle(version.getTitle());
        document.setContent(version.getContent());
        document.setSummary(version.getSummary());
        documentMapper.updateById(document);

        return true;
    }

    @Override
    public String compareVersions(Long versionId1, Long versionId2) {
        if (versionId1 == null || versionId2 == null) {
            throw new BusinessException("Version ID must not be empty");
        }

        if (versionId1.equals(versionId2)) {
            throw new BusinessException("Cannot compare the same version");
        }

        DocumentVersion version1 = documentVersionMapper.selectById(versionId1);
        DocumentVersion version2 = documentVersionMapper.selectById(versionId2);

        if (version1 == null || version2 == null) {
            throw new BusinessException("Version does not exist");
        }

        // Build the diff comparison result
        StringBuilder diff = new StringBuilder();
        diff.append("=== Version Comparison ===\n");
        diff.append(String.format("Version %d vs Version %d\n", version1.getVersion(), version2.getVersion()));
        diff.append("\n");

        // Title differences
        if (!version1.getTitle().equals(version2.getTitle())) {
            diff.append("[Title Difference]\n");
            diff.append(String.format("- Version %d: %s\n", version1.getVersion(), version1.getTitle()));
            diff.append(String.format("+ Version %d: %s\n", version2.getVersion(), version2.getTitle()));
            diff.append("\n");
        }

        // Content differences (simple implementation; a diff library could be used in practice)
        String content1 = version1.getContent() != null ? version1.getContent() : "";
        String content2 = version2.getContent() != null ? version2.getContent() : "";

        if (!content1.equals(content2)) {
            diff.append("[Content Difference]\n");
            diff.append(String.format("Version %d content length: %d characters\n", version1.getVersion(), content1.length()));
            diff.append(String.format("Version %d content length: %d characters\n", version2.getVersion(), content2.length()));
            diff.append(String.format("Diff size: %d characters\n", content2.length() - content1.length()));
        }

        return diff.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVersion(Long versionId, Long userId) {
        log.info("Delete document version: versionId={}, userId={}", versionId, userId);

        if (versionId == null) {
            throw new BusinessException("Version ID must not be empty");
        }

        // Check whether the version exists
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        // Check whether it is the latest version
        Long count = documentVersionMapper.selectCount(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, version.getDocumentId())
                        .gt(DocumentVersion::getVersion, version.getVersion())
        );

        if (count > 0) {
            throw new BusinessException("Cannot delete an intermediate version; only the latest version can be deleted");
        }

        // Delete the version
        int deleteCount = documentVersionMapper.deleteById(versionId);
        return deleteCount > 0;
    }

    /**
     * Convert to VO
     *
     * @param version the version entity
     * @return the version VO
     */
    private DocumentVersionVO convertToVO(DocumentVersion version) {
        return DocumentVersionVO.builder()
                .id(version.getId())
                .documentId(version.getDocumentId())
                .version(version.getVersion())
                .title(version.getTitle())
                .changeDescription(version.getChangeDescription())
                .changeSize(version.getChangeSize())
                .operatorId(version.getOperatorId())
                .operatorName(version.getOperatorName())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
```

### 6.3 Core Method Notes

#### 6.3.1 Version Creation Logic

Key steps in version creation:

1. Get the current maximum version number for the document
2. Compute the change size relative to the previous version
3. Create the new version record

**Design advantages:**
- Automatically incrementing version numbers
- Recording the change size to facilitate analysis
- Saving a complete document snapshot

#### 6.3.2 Version Restore Logic

Version restore uses a "backup before restore" strategy:

```java
// Create a backup of the current version (before restoring)
createVersion(documentId, "Automatic backup before restore", userId);

// Restore the document content
document.setTitle(version.getTitle());
document.setContent(version.getContent());
document.setSummary(version.getSummary());
```

**Design advantages:**
- Prevents data loss from accidental operations
- Preserves the complete modification history
- Supports rolling back to the state before the restore

---

## VII. Controller Layer

### 7.1 Create DocumentVersionController

Create `kb-document/src/main/java/com/knowledge/base/document/controller/DocumentVersionController.java`:

```java
package com.knowledge.base.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.service.DocumentVersionService;
import com.knowledge.base.document.vo.DocumentVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Document version management Controller
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; provides
 * document version management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/documents/{documentId}/versions")
@Tag(name = "Document Version Management", description = "Document version management endpoints")
public class DocumentVersionController {

    @Resource
    private DocumentVersionService documentVersionService;

    /**
     * Get the document version list
     *
     * @param documentId the document ID
     * @param current    the current page
     * @param size       the page size
     * @return paginated version information
     */
    @GetMapping
    @Operation(summary = "Get document version list", description = "Paginated query of the document version list")
    public Result<IPage<DocumentVersionVO>> getVersions(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size) {
        log.info("Get document version list request: documentId={}, current={}, size={}", documentId, current, size);

        IPage<DocumentVersionVO> page = documentVersionService.getVersionList(documentId, current, size);
        return Result.success(page);
    }

    /**
     * Get document version details
     *
     * @param documentId the document ID
     * @param versionId  the version ID
     * @return the version details
     */
    @GetMapping("/{versionId}")
    @Operation(summary = "Get document version details", description = "Get version details by version ID")
    public Result<DocumentVersionVO> getVersionDetail(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Version ID", required = true)
        @PathVariable Long versionId) {
        log.info("Get document version details request: documentId={}, versionId={}", documentId, versionId);

        DocumentVersionVO versionVO = documentVersionService.getVersionDetail(versionId);
        return Result.success(versionVO);
    }

    /**
     * Restore a document version
     *
     * @param documentId the document ID
     * @param dto        the restore version DTO
     * @return whether it succeeded
     */
    @PostMapping("/restore")
    @Operation(summary = "Restore document version", description = "Restore the document to the specified version")
    public Result<Boolean> restoreVersion(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Valid @RequestBody DocumentVersionRestoreDTO dto) {
        log.info("Restore document version request: documentId={}, versionId={}", documentId, dto.getVersionId());

        // TODO: get the current user ID from the context
        Long userId = 1L;
        Boolean success = documentVersionService.restoreVersion(documentId, dto, userId);
        return Result.success("Version restored successfully", success);
    }

    /**
     * Compare two versions
     *
     * @param documentId the document ID
     * @param versionId1 version 1 ID
     * @param versionId2 version 2 ID
     * @return the comparison result
     */
    @GetMapping("/compare")
    @Operation(summary = "Compare document versions", description = "Compare the differences between two document versions")
    public Result<String> compareVersions(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Version 1 ID", required = true)
        @RequestParam Long versionId1,
        @Parameter(description = "Version 2 ID", required = true)
        @RequestParam Long versionId2) {
        log.info("Compare document versions request: documentId={}, versionId1={}, versionId2={}",
            documentId, versionId1, versionId2);

        String diff = documentVersionService.compareVersions(versionId1, versionId2);
        return Result.success(diff);
    }
}
```

### 7.2 RESTful API Design

| Method | Path | Description | Parameters |
|------|------|------|------|
| GET | /documents/{id}/versions | Get the version list | documentId, current, size |
| GET | /documents/{id}/versions/{versionId} | Get version details | documentId, versionId |
| POST | /documents/{id}/versions/restore | Restore a version | documentId, RestoreDTO |
| GET | /documents/{id}/versions/compare | Compare versions | documentId, versionId1, versionId2 |

---

## VIII. Integrating with the Document Service

### 8.1 Integrating Version Creation into DocumentService

Automatically create a version when a document is updated, by modifying the `updateDocument` method of `DocumentServiceImpl`:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean updateDocument(DocumentDTO documentDTO) {
    log.info("Update document: documentId={}", documentDTO.getId());

    if (documentDTO.getId() == null) {
        throw new BusinessException("Document ID must not be empty");
    }

    // Get the document before the update (used to create the version)
    Document oldDocument = documentMapper.selectById(documentDTO.getId());
    if (oldDocument == null) {
        throw new BusinessException("Document does not exist");
    }

    // Update the document
    Document document = new Document();
    BeanUtil.copyProperties(documentDTO, document);

    // If the status changes from draft to published, set the publish time
    if (Objects.equals(oldDocument.getStatus(), 0)
        && Objects.equals(documentDTO.getStatus(), 1)) {
        document.setPublishTime(LocalDateTime.now());
    }

    int count = documentMapper.updateById(document);

    // Automatically create a version
    if (count > 0) {
        // TODO: get the current user ID from the context
        Long userId = 1L;
        String changeDescription = buildChangeDescription(oldDocument, document);
        documentVersionService.createVersion(documentDTO.getId(), changeDescription, userId);
    }

    return count > 0;
}

/**
 * Build the change description
 */
private String buildChangeDescription(Document oldDocument, Document newDocument) {
    List<String> changes = new ArrayList<>();

    if (!Objects.equals(oldDocument.getTitle(), newDocument.getTitle())) {
        changes.add("Title changed");
    }
    if (!Objects.equals(oldDocument.getContent(), newDocument.getContent())) {
        changes.add("Content updated");
    }
    if (!Objects.equals(oldDocument.getCategoryId(), newDocument.getCategoryId())) {
        changes.add("Category adjusted");
    }

    return changes.isEmpty() ? "Document updated" : String.join(", ", changes);
}
```

---

## IX. Testing and Verification

### 9.1 Test the Version Feature

#### Create a Document and Generate a Version

```bash
# 1. Create a document (version 1 is generated automatically)
curl -X POST http://localhost:8082/api/document/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started with Spring Boot 3.x",
    "summary": "This article introduces the basics of Spring Boot 3.x",
    "content": "Spring Boot 3.x is built on Spring Framework 6.x...",
    "documentType": 1,
    "status": 1
  }'

# 2. Update the document (version 2 is generated automatically)
curl -X PUT http://localhost:8082/api/document/documents \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "title": "Getting Started with Spring Boot 3.x (Updated)",
    "content": "Spring Boot 3.x is built on Spring Framework 6.x... (updated content)"
  }'
```

#### Get the Version List

```bash
curl "http://localhost:8082/api/document/documents/1/versions?current=1&size=10"
```

Example response:

```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": {
    "records": [
      {
        "id": 1000000000000000001,
        "documentId": 1,
        "version": 2,
        "title": "Getting Started with Spring Boot 3.x (Updated)",
        "changeDescription": "Content updated",
        "changeSize": 256,
        "operatorId": 1,
        "operatorName": "System User",
        "createdAt": "2026-05-13T14:30:00"
      },
      {
        "id": 1000000000000000000,
        "documentId": 1,
        "version": 1,
        "title": "Getting Started with Spring Boot 3.x",
        "changeDescription": null,
        "changeSize": 0,
        "operatorId": 1,
        "operatorName": "System User",
        "createdAt": "2026-05-13T14:00:00"
      }
    ],
    "total": 2,
    "current": 1,
    "size": 10
  }
}
```

#### Restore a Version

```bash
curl -X POST http://localhost:8082/api/document/documents/1/versions/restore \
  -H "Content-Type: application/json" \
  -d '{
    "versionId": 1000000000000000000,
    "reason": "Need to roll back to the initial version"
  }'
```

#### Compare Versions

```bash
curl "http://localhost:8082/api/document/documents/1/versions/compare?versionId1=1000000000000000000&versionId2=1000000000000000001"
```

Example response:

```
=== Version Comparison ===
Version 1 vs Version 2

[Title Difference]
- Version 1: Getting Started with Spring Boot 3.x
+ Version 2: Getting Started with Spring Boot 3.x (Updated)

[Content Difference]
Version 1 content length: 5000 characters
Version 2 content length: 5256 characters
Diff size: 256 characters
```

---

## X. Summary

This article described in detail the complete implementation of the document version management feature, covering the following core content:

### Completed Features

| Feature | Implementation | Description |
|------|---------|------|
| Version creation | Automatically triggered | Automatically created when the document is updated |
| Version list | Paginated query | Sorted by version number descending |
| Version detail | Full query | Includes title, content, operator, etc. |
| Version restore | Restore + backup | Automatically backs up the current version |
| Version comparison | Text comparison | Shows title and content differences |
| Version deletion | Latest version only | Only the latest version can be deleted |

### Technical Highlights

1. **Automatic version creation**: integrated into DocumentService
2. **Version number management**: auto-incrementing, globally unique
3. **Change size statistics**: records the diff relative to the previous version
4. **Backup before restore**: prevents data loss from accidental operations

### Design Highlights

1. **Automatic backup before restore**: protects user data safety
2. **Version comparison feature**: intuitively displays version differences
3. **Version deletion restriction**: only the latest version can be deleted
4. **Comprehensive Mapper methods**: supports a variety of query scenarios

Through this article, you should now be able to grasp:
- How to design a version management system
- The implementation principles of document version control
- The business logic behind version restoration and comparison
- Advanced MyBatis Plus query techniques

Happy building!
