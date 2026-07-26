# Adding the Document Service Feature

## I. Overview

The document service (kb-document) is the core module of the enterprise knowledge base system, responsible for the full lifecycle management of documents: creation, editing, publishing, querying, commenting, review, and more. This article describes in detail how to build the document service module from scratch.

### 1.1 Service Positioning

The document service, as the system's core business module, provides the following features:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Document management | Document CRUD, publish, archive | Knowledge base content management |
| Category management | Tree-structured category hierarchy | Organizing and classifying documents |
| Tag management | Tag CRUD, associations | Tagging and retrieving documents |
| Comment management | Comments, replies, likes | User interaction |
| Version management | Document version history | Version rollback and comparison |
| Review management | Document review workflow | Content quality control |
| File upload | Document file upload | Office, PDF, and other files |

### 1.2 Technical Architecture

```
kb-document
├── Data persistence layer: MyBatis Plus + MySQL
├── Cache layer: Redis (document view counts, hot data)
├── Search layer: Elasticsearch (full-text search)
├── File processing: Apache POI, PDFBox
├── API docs: Knife4j
└── Infrastructure: Spring Boot 3.2
```

### 1.3 Core Entity Relationships

```
Document
    ├── Category - many-to-one
    ├── Tag - many-to-many
    ├── Comment - one-to-many
    ├── DocumentVersion - one-to-many
    └── DocumentReview - one-to-many
```

---

## II. Environment Setup

### 2.1 Create the Database

```bash
mysql -u root -p -e "CREATE DATABASE kb_document DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 2.2 Install Elasticsearch (Optional)

The document service supports full-text search, which requires Elasticsearch to be installed:

```bash
# Install Elasticsearch using Docker
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:8.11.0
```

### 2.3 Create the Upload Directory

```bash
# Create the file upload directory
mkdir -p /data/knowledge-base/uploads

# Set permissions
chmod 755 /data/knowledge-base/uploads
```

---

## III. Creating the Module Skeleton

### 3.1 Create the Maven Module

```bash
mkdir -p kb-document/src/main/java/com/knowledge/base/document
mkdir -p kb-document/src/main/resources/mapper
```

### 3.2 Configure pom.xml

Create `kb-document/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.knowledge.base</groupId>
        <artifactId>knowledge-base-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>kb-document</artifactId>
    <packaging>jar</packaging>
    <name>Knowledge Base Document Service</name>
    <description>Document Service (core)</description>

    <dependencies>
        <!-- Common module -->
        <dependency>
            <groupId>com.knowledge.base</groupId>
            <artifactId>kb-common</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MySQL driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Druid data source -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Elasticsearch -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Apache POI (document processing) -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>

        <!-- PDFBox (PDF processing) -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>2.0.29</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Knife4j -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## IV. Database Design

### 4.1 Document Table (kb_document)

```sql
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL COMMENT 'Document ID',
  `title` VARCHAR(200) NOT NULL COMMENT 'Document title',
  `content` LONGTEXT NOT NULL COMMENT 'Document content',
  `summary` TEXT DEFAULT NULL COMMENT 'Document summary',
  `category_id` BIGINT DEFAULT NULL COMMENT 'Category ID',
  `author_id` BIGINT NOT NULL COMMENT 'Author ID',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT 'Author name (denormalized field)',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT 'Cover image',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'Status',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether public',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether pinned',
  `allow_comment` TINYINT NOT NULL DEFAULT 1 COMMENT 'Allow comments',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT 'View count',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT 'Comment count',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT 'Favorite count',
  `version` INT NOT NULL DEFAULT 1 COMMENT 'Version number',
  `word_count` INT DEFAULT NULL COMMENT 'Word count',
  `publish_time` DATETIME DEFAULT NULL COMMENT 'Publish time',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`(100)),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  FULLTEXT KEY `ft_content` (`title`, `content`, `summary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document table';
```

### 4.2 Category Table (kb_category)

```sql
CREATE TABLE `kb_category` (
  `id` BIGINT NOT NULL COMMENT 'Category ID',
  `category_name` VARCHAR(50) NOT NULL COMMENT 'Category name',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent category ID',
  `category_icon` VARCHAR(50) DEFAULT '📁' COMMENT 'Category icon',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Category description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `document_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document category table';
```

### 4.3 Tag Table (tb_tag)

```sql
CREATE TABLE `tb_tag` (
  `id` BIGINT NOT NULL COMMENT 'Tag ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT 'Tag name',
  `tag_code` VARCHAR(50) DEFAULT NULL COMMENT 'Tag code',
  `tag_type` VARCHAR(20) DEFAULT 'USER' COMMENT 'Tag type: SYSTEM-system tag, USER-user tag',
  `color` VARCHAR(20) DEFAULT NULL COMMENT 'Tag color',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Tag icon',
  `doc_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tag table';
```

### 4.4 Comment Table (tb_comment)

```sql
CREATE TABLE `tb_comment` (
  `id` BIGINT NOT NULL COMMENT 'Comment ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `content` TEXT NOT NULL COMMENT 'Comment content',
  `user_id` BIGINT NOT NULL COMMENT 'Commenting user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (denormalized field)',
  `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT 'User avatar (denormalized field)',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent comment ID',
  `root_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Root comment ID',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT 'ID of the user being replied to',
  `reply_to_user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Who this replies to (denormalized field)',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT 'Reply count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Delete flag',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comment table';
```

### 4.5 Document Version Table (tb_document_version)

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
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document version table';
```

### 4.6 Document Review Table (tb_document_review)

```sql
CREATE TABLE `tb_document_review` (
  `id` BIGINT NOT NULL COMMENT 'Review ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `reviewer_id` BIGINT NOT NULL COMMENT 'Reviewer ID',
  `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT 'Reviewer name',
  `review_result` INT NOT NULL COMMENT 'Review result: 1-approved, 2-rejected',
  `review_comment` TEXT DEFAULT NULL COMMENT 'Review comment',
  `before_status` INT DEFAULT NULL COMMENT 'Status before review',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT 'Review time',
  `review_round` INT NOT NULL DEFAULT 1 COMMENT 'Review round',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document review table';
```

### 4.7 Document-Tag Association Table (kb_document_tag)

```sql
CREATE TABLE `kb_document_tag` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `tag_id` BIGINT NOT NULL COMMENT 'Tag ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_tag` (`document_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document-tag association table';
```

---

## V. Application Configuration

### 5.1 Create the Bootstrap Class

Create `kb-document/src/main/java/com/knowledge/base/document/DocumentApplication.java`:

```java
package com.knowledge.base.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Document service bootstrap class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.knowledge.base.document", "com.knowledge.base.common"})
public class DocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentApplication.class, args);
        System.out.println("========================================");
        System.out.println("Document service started successfully!");
        System.out.println("Swagger doc URL: http://localhost:8082/api/document/doc.html");
        System.out.println("========================================");
    }
}
```

### 5.2 Configure Knife4j

Create `kb-document/src/main/java/com/knowledge/base/document/config/Knife4jConfig.java`:

```java
package com.knowledge.base.document.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j configuration class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; configures
 * the API docs</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * Configure OpenAPI
     *
     * @return the OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Knowledge Base System - Document Service API Docs")
                .version("1.0.0")
                .description("Provides document management, document search, document categorization, and related features")
                .contact(new Contact()
                    .name("airwzz999")
                    .email("support@knowledge-base.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
```

### 5.3 Configure application.yml

Create `kb-document/src/main/resources/application.yml`:

```yaml
server:
  port: 8082
  servlet:
    context-path: /api/document

spring:
  application:
    name: kb-document

  # Data source configuration
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/kb_document?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    druid:
      initial-size: 20
      min-idle: 10
      max-active: 100
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      filters: stat,wall
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: admin

  # Redis configuration (optional)
  redis:
    host: localhost
    port: 6379
    password:
    database: 1
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

  # Servlet configuration
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB

  # Jackson configuration
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

  # Elasticsearch configuration
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 10s
    socket-timeout: 30s

# MyBatis Plus configuration
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.knowledge.base.document.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: input
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# Logging configuration
logging:
  level:
    root: INFO
    com.knowledge.base: DEBUG
    com.baomidou.mybatisplus: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"

# File storage configuration
file:
  upload:
    path: /data/knowledge-base/uploads
    allowed-types:
      - pdf
      - doc
      - docx
      - xls
      - xlsx
      - ppt
      - pptx
      - txt
      - md
    max-size: 104857600

# Swagger configuration
knife4j:
  enable: true
  setting:
    language: zh_cn
  production: false
```

---

## VI. Core Feature Implementation

### 6.1 Document Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/Document.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Document entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_document")
public class Document extends BaseEntity {

    /**
     * Document title
     */
    private String title;

    /**
     * Document summary
     */
    private String summary;

    /**
     * Document content
     */
    private String content;

    /**
     * Document type (1-article, 2-file)
     */
    private Integer documentType;

    /**
     * File path
     */
    private String filePath;

    /**
     * File size (bytes)
     */
    private Long fileSize;

    /**
     * File extension
     */
    private String fileExtension;

    /**
     * MIME type
     */
    private String mimeType;

    /**
     * Category ID
     */
    private Long categoryId;

    /**
     * Tags (comma-separated)
     */
    private String tags;

    /**
     * Status (0-draft, 1-published, 2-archived)
     */
    private Integer status;

    /**
     * Whether pinned (0-no, 1-yes)
     */
    private Integer isTop;

    /**
     * Whether recommended (0-no, 1-yes)
     */
    private Integer isRecommend;

    /**
     * View count
     */
    private Long viewCount;

    /**
     * Like count
     */
    private Long likeCount;

    /**
     * Favorite count
     */
    private Long favoriteCount;

    /**
     * Comment count
     */
    private Long commentCount;

    /**
     * Publish time
     */
    private LocalDateTime publishTime;

    /**
     * Author ID
     */
    private Long authorId;

    /**
     * Author name
     */
    private String authorName;

    /**
     * Cover image URL
     */
    private String coverImage;

    /**
     * Source (1-original, 2-reposted, 3-translated)
     */
    private Integer source;

    /**
     * Source URL
     */
    private String sourceUrl;

    /**
     * Allow comments (0-no, 1-yes)
     */
    private Integer allowComment;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Remarks
     */
    private String remark;
}
```

### 6.2 Document DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/DocumentDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Document DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document information request parameters")
public class DocumentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Document ID")
    private Long id;

    @Schema(description = "Document title", required = true)
    @NotBlank(message = "Document title must not be empty")
    @Size(max = 200, message = "Document title must not exceed 200 characters")
    private String title;

    @Schema(description = "Document summary")
    @Size(max = 500, message = "Document summary must not exceed 500 characters")
    private String summary;

    @Schema(description = "Document content")
    private String content;

    @Schema(description = "Document type (1-article, 2-file)")
    private Integer documentType;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Tags (comma-separated)")
    @Size(max = 200, message = "Tags must not exceed 200 characters")
    private String tags;

    @Schema(description = "Status (0-draft, 1-published, 2-archived)")
    private Integer status;

    @Schema(description = "Whether pinned (0-no, 1-yes)")
    private Integer isTop;

    @Schema(description = "Whether recommended (0-no, 1-yes)")
    private Integer isRecommend;

    @Schema(description = "Cover image URL")
    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    private String coverImage;

    @Schema(description = "Source (1-original, 2-reposted, 3-translated)")
    private Integer source;

    @Schema(description = "Source URL")
    @Size(max = 500, message = "Source URL must not exceed 500 characters")
    private String sourceUrl;

    @Schema(description = "Allow comments (0-no, 1-yes)")
    private Integer allowComment;

    @Schema(description = "Sort order")
    private Integer sort;

    @Schema(description = "Remarks")
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remark;
}
```

### 6.3 Document Query DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/DocumentQueryDTO.java`:

```java
package com.knowledge.base.document.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Document query DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used for
 * document query conditions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Document query parameters")
public class DocumentQueryDTO extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Category ID
     */
    @Schema(description = "Category ID")
    private Long categoryId;

    /**
     * Tags (comma-separated)
     */
    @Schema(description = "Tags")
    private String tags;

    /**
     * Status: 0-draft, 1-published, 2-archived
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Document type: 1-article, 2-file
     */
    @Schema(description = "Document type")
    private Integer documentType;

    /**
     * Keyword search (title or content)
     */
    @Schema(description = "Keyword")
    private String keyword;

    /**
     * Author ID
     */
    @Schema(description = "Author ID")
    private Long authorId;

    /**
     * Whether pinned: 0-no, 1-yes
     */
    @Schema(description = "Whether pinned")
    private Integer isTop;

    /**
     * Whether recommended: 0-no, 1-yes
     */
    @Schema(description = "Whether recommended")
    private Integer isRecommend;

    /**
     * Start time
     */
    @Schema(description = "Start time")
    private String startTime;

    /**
     * End time
     */
    @Schema(description = "End time")
    private String endTime;
}
```

### 6.5 Document VO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/DocumentVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document response VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document information response")
public class DocumentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Document ID")
    private Long id;

    @Schema(description = "Document title")
    private String title;

    @Schema(description = "Document summary")
    private String summary;

    @Schema(description = "Document content")
    private String content;

    @Schema(description = "Document type")
    private Integer documentType;

    @Schema(description = "File path")
    private String filePath;

    @Schema(description = "File size")
    private Long fileSize;

    @Schema(description = "File extension")
    private String fileExtension;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Tag list")
    private String tags;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Whether pinned")
    private Integer isTop;

    @Schema(description = "Whether recommended")
    private Integer isRecommend;

    @Schema(description = "View count")
    private Long viewCount;

    @Schema(description = "Like count")
    private Long likeCount;

    @Schema(description = "Favorite count")
    private Long favoriteCount;

    @Schema(description = "Comment count")
    private Long commentCount;

    @Schema(description = "Publish time")
    private LocalDateTime publishTime;

    @Schema(description = "Author ID")
    private Long authorId;

    @Schema(description = "Author name")
    private String authorName;

    @Schema(description = "Cover image URL")
    private String coverImage;

    @Schema(description = "Source")
    private Integer source;

    @Schema(description = "Source URL")
    private String sourceUrl;

    @Schema(description = "Allow comments")
    private Integer allowComment;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
```

### 6.6 Document Mapper

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/DocumentMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Document Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * Increment the view count
     *
     * @param documentId the document ID
     * @return the number of affected rows
     */
    int incrementViewCount(@Param("documentId") Long documentId);

    /**
     * Increment the like count
     *
     * @param documentId the document ID
     * @return the number of affected rows
     */
    int incrementLikeCount(@Param("documentId") Long documentId);

    /**
     * Increment the favorite count
     *
     * @param documentId the document ID
     * @return the number of affected rows
     */
    int incrementFavoriteCount(@Param("documentId") Long documentId);

    /**
     * Increment the comment count
     *
     * @param documentId the document ID
     * @return the number of affected rows
     */
    int incrementCommentCount(@Param("documentId") Long documentId);
}
```

Create `kb-document/src/main/resources/mapper/DocumentMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.DocumentMapper">

    <update id="incrementViewCount">
        UPDATE kb_document
        SET view_count = view_count + 1
        WHERE id = #{documentId} AND deleted = 0
    </update>

    <update id="incrementLikeCount">
        UPDATE kb_document
        SET like_count = like_count + 1
        WHERE id = #{documentId} AND deleted = 0
    </update>

    <update id="incrementFavoriteCount">
        UPDATE kb_document
        SET collect_count = collect_count + 1
        WHERE id = #{documentId} AND deleted = 0
    </update>

    <update id="incrementCommentCount">
        UPDATE kb_document
        SET comment_count = comment_count + 1
        WHERE id = #{documentId} AND deleted = 0
    </update>

</mapper>
```

### 6.7 Document Service

Create the interface `kb-document/src/main/java/com/knowledge/base/document/service/DocumentService.java`:

```java
package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentService extends IService<Document> {

    /**
     * Create a document
     *
     * @param documentDTO the document information
     * @return the document ID
     */
    Long createDocument(DocumentDTO documentDTO);

    /**
     * Update a document
     *
     * @param documentDTO the document information
     * @return whether it succeeded
     */
    Boolean updateDocument(DocumentDTO documentDTO);

    /**
     * Delete a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean deleteDocument(Long documentId);

    /**
     * Query a document by ID
     *
     * @param documentId the document ID
     * @return the document information
     */
    DocumentVO getDocumentById(Long documentId);

    /**
     * View a document (increments the view count)
     *
     * @param documentId the document ID
     * @return the document information
     */
    DocumentVO viewDocument(Long documentId);

    /**
     * Paginated query of the document list
     *
     * @param current    the current page
     * @param size       the page size
     * @param categoryId the category ID
     * @param keyword    the search keyword
     * @param status     the status
     * @return paginated document information
     */
    IPage<DocumentVO> pageDocuments(Long current, Long size, Long categoryId, String keyword, Integer status);

    /**
     * Upload a document file
     *
     * @param file the file
     * @return the file path
     */
    String uploadDocumentFile(MultipartFile file);

    /**
     * Like a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean likeDocument(Long documentId);

    /**
     * Favorite a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean favoriteDocument(Long documentId);

    /**
     * Publish a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean publishDocument(Long documentId);

    /**
     * Archive a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean archiveDocument(Long documentId);
}
```

Create the implementation class `kb-document/src/main/java/com/knowledge/base/document/service/impl/DocumentServiceImpl.java`:

```java
package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.ResultCode;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.vo.DocumentVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Document Service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    @Resource
    private DocumentMapper documentMapper;

    @Value("${file.upload.path:/data/knowledge-base/uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:104857600}")
    private Long maxFileSize;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(DocumentDTO documentDTO) {
        log.info("Create document: title={}", documentDTO.getTitle());

        // Build the document entity
        Document document = new Document();
        BeanUtil.copyProperties(documentDTO, document);

        // Generate the ID
        document.setId(SnowflakeIdGenerator.getInstance().nextId());

        // Set default values
        if (document.getDocumentType() == null) {
            document.setDocumentType(1);
        }
        if (document.getStatus() == null) {
            document.setStatus(0);
        }
        if (document.getIsTop() == null) {
            document.setIsTop(0);
        }
        if (document.getIsRecommend() == null) {
            document.setIsRecommend(0);
        }
        if (document.getSource() == null) {
            document.setSource(1);
        }
        if (document.getAllowComment() == null) {
            document.setAllowComment(1);
        }
        if (document.getSort() == null) {
            document.setSort(0);
        }
        if (document.getViewCount() == null) {
            document.setViewCount(0L);
        }
        if (document.getLikeCount() == null) {
            document.setLikeCount(0L);
        }
        if (document.getFavoriteCount() == null) {
            document.setFavoriteCount(0L);
        }
        if (document.getCommentCount() == null) {
            document.setCommentCount(0L);
        }

        // TODO: get the current logged-in user from the context
        document.setAuthorId(1L);
        document.setAuthorName("System Administrator");

        // If the status is published, set the publish time
        if (Objects.equals(document.getStatus(), 1)) {
            document.setPublishTime(LocalDateTime.now());
        }

        // Save the document
        int count = documentMapper.insert(document);
        if (count <= 0) {
            throw new BusinessException("Failed to create document");
        }

        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDocument(DocumentDTO documentDTO) {
        log.info("Update document: documentId={}", documentDTO.getId());

        if (documentDTO.getId() == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document existDocument = documentMapper.selectById(documentDTO.getId());
        if (existDocument == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // Build the update entity
        Document document = new Document();
        BeanUtil.copyProperties(documentDTO, document);

        // If the status changes from draft to published, set the publish time
        if (Objects.equals(existDocument.getStatus(), 0)
            && Objects.equals(documentDTO.getStatus(), 1)) {
            document.setPublishTime(LocalDateTime.now());
        }

        int count = documentMapper.updateById(document);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDocument(Long documentId) {
        log.info("Delete document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        int count = documentMapper.deleteById(documentId);
        return count > 0;
    }

    @Override
    public DocumentVO getDocumentById(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        return BeanUtil.copyProperties(document, DocumentVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO viewDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // Increment the view count
        documentMapper.incrementViewCount(documentId);

        return BeanUtil.copyProperties(document, DocumentVO.class);
    }

    @Override
    public IPage<DocumentVO> pageDocuments(Long current, Long size, Long categoryId, String keyword, Integer status) {
        // Build the query conditions
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            wrapper.eq(Document::getCategoryId, categoryId);
        }

        if (status != null) {
            wrapper.eq(Document::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Document::getTitle, keyword)
                .or()
                .like(Document::getSummary, keyword)
                .or()
                .like(Document::getContent, keyword)
                .or()
                .like(Document::getTags, keyword));
        }

        // Sort by pinned status and sort order
        wrapper.orderByDesc(Document::getIsTop)
            .orderByDesc(Document::getSort)
            .orderByDesc(Document::getPublishTime);

        // Paginated query
        Page<Document> page = new Page<>(current, size);
        IPage<Document> documentPage = documentMapper.selectPage(page, wrapper);

        // Convert to VO
        return documentPage.convert(document -> BeanUtil.copyProperties(document, DocumentVO.class));
    }

    @Override
    public String uploadDocumentFile(MultipartFile file) {
        log.info("Upload document file: fileName={}", file.getOriginalFilename());

        // Check whether the file is empty
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        // Check the file size
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // Get the original filename and extension
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename);

        // Check the file type
        if (!StrUtil.isNotBlank(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }

        try {
            // Generate a unique filename
            String fileName = IdUtil.simpleUUID() + "." + extension;

            // Create the upload directory
            String datePath = LocalDateTime.now().toLocalDate().toString();
            String fullPath = uploadPath + File.separator + datePath;
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save the file
            File destFile = new File(fullPath, fileName);
            file.transferTo(destFile);

            // Return the relative path
            return datePath + File.separator + fileName;
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean likeDocument(Long documentId) {
        log.info("Like document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // TODO: check whether the user has already liked it

        // Increment the like count
        int count = documentMapper.incrementLikeCount(documentId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean favoriteDocument(Long documentId) {
        log.info("Favorite document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_EXIST);
        }

        // TODO: check whether the user has already favorited it

        // Increment the favorite count
        int count = documentMapper.incrementFavoriteCount(documentId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishDocument(Long documentId) {
        log.info("Publish document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        Document document = new Document();
        document.setId(documentId);
        document.setStatus(1);
        document.setPublishTime(LocalDateTime.now());

        int count = documentMapper.updateById(document);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean archiveDocument(Long documentId) {
        log.info("Archive document: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        Document document = new Document();
        document.setId(documentId);
        document.setStatus(2);

        int count = documentMapper.updateById(document);
        return count > 0;
    }
}
```

### 6.8 Document Controller

Create `kb-document/src/main/java/com/knowledge/base/document/controller/DocumentController.java`:

```java
package com.knowledge.base.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@Tag(name = "Document Management", description = "Document information management endpoints")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    /**
     * Create a document
     *
     * @param documentDTO the document information
     * @return the document ID
     */
    @PostMapping
    @Operation(summary = "Create document", description = "Create a new document")
    public Result<Long> createDocument(@Valid @RequestBody DocumentDTO documentDTO) {
        log.info("Create document request: title={}", documentDTO.getTitle());

        Long documentId = documentService.createDocument(documentDTO);
        return Result.success("Document created successfully", documentId);
    }

    /**
     * Update a document
     *
     * @param documentDTO the document information
     * @return whether it succeeded
     */
    @PutMapping
    @Operation(summary = "Update document", description = "Update document information")
    public Result<Boolean> updateDocument(@Valid @RequestBody DocumentDTO documentDTO) {
        log.info("Update document request: documentId={}", documentDTO.getId());

        Boolean success = documentService.updateDocument(documentDTO);
        return Result.success("Document updated successfully", success);
    }

    /**
     * Delete a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Delete a document by document ID")
    public Result<Boolean> deleteDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Delete document request: documentId={}", documentId);

        Boolean success = documentService.deleteDocument(documentId);
        return Result.success("Document deleted successfully", success);
    }

    /**
     * Query a document by ID
     *
     * @param documentId the document ID
     * @return the document information
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Query document", description = "Query document details by document ID")
    public Result<DocumentVO> getDocumentById(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Query document request: documentId={}", documentId);

        DocumentVO documentVO = documentService.getDocumentById(documentId);
        return Result.success(documentVO);
    }

    /**
     * View a document (increments the view count)
     *
     * @param documentId the document ID
     * @return the document information
     */
    @GetMapping("/{documentId}/view")
    @Operation(summary = "View document", description = "View a document and increment its view count")
    public Result<DocumentVO> viewDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("View document request: documentId={}", documentId);

        DocumentVO documentVO = documentService.viewDocument(documentId);
        return Result.success(documentVO);
    }

    /**
     * Paginated query of the document list
     *
     * @param current    the current page
     * @param size       the page size
     * @param categoryId the category ID
     * @param keyword    the search keyword
     * @param status     the status
     * @return paginated document information
     */
    @GetMapping("/page")
    @Operation(summary = "Paginated query of documents", description = "Paginated query of the document list")
    public Result<IPage<DocumentVO>> pageDocuments(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Category ID") @RequestParam(required = false) Long categoryId,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
        @Parameter(description = "Status") @RequestParam(required = false) Integer status) {
        log.info("Paginated document query request: current={}, size={}, categoryId={}, keyword={}, status={}",
            current, size, categoryId, keyword, status);

        IPage<DocumentVO> page = documentService.pageDocuments(current, size, categoryId, keyword, status);
        return Result.success(page);
    }

    /**
     * Upload a document file
     *
     * @param file the file
     * @return the file path
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload document file", description = "Upload a document file and return its file path")
    public Result<String> uploadDocumentFile(
        @Parameter(description = "File", required = true)
        @RequestParam("file") MultipartFile file) {
        log.info("Upload document file request: fileName={}", file.getOriginalFilename());

        String filePath = documentService.uploadDocumentFile(file);
        return Result.success("File uploaded successfully", filePath);
    }

    /**
     * Like a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @PostMapping("/{documentId}/like")
    @Operation(summary = "Like document", description = "User likes a document")
    public Result<Boolean> likeDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Like document request: documentId={}", documentId);

        Boolean success = documentService.likeDocument(documentId);
        return Result.success("Liked successfully", success);
    }

    /**
     * Favorite a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @PostMapping("/{documentId}/favorite")
    @Operation(summary = "Favorite document", description = "User favorites a document")
    public Result<Boolean> favoriteDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Favorite document request: documentId={}", documentId);

        Boolean success = documentService.favoriteDocument(documentId);
        return Result.success("Favorited successfully", success);
    }

    /**
     * Publish a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @PutMapping("/{documentId}/publish")
    @Operation(summary = "Publish document", description = "Publish a document")
    public Result<Boolean> publishDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Publish document request: documentId={}", documentId);

        Boolean success = documentService.publishDocument(documentId);
        return Result.success("Published successfully", success);
    }

    /**
     * Archive a document
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    @PutMapping("/{documentId}/archive")
    @Operation(summary = "Archive document", description = "Archive a document")
    public Result<Boolean> archiveDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Archive document request: documentId={}", documentId);

        Boolean success = documentService.archiveDocument(documentId);
        return Result.success("Archived successfully", success);
    }
}
```

---

## VII. Other Core Features

Due to space constraints, the other features are implemented in a way similar to document management, including:

### 7.1 Category Management

- **Entity**: `Category.java`
- **DTO**: `CategoryDTO.java`
- **VO**: `CategoryVO.java`
- **Service**: `CategoryService.java`
- **Controller**: `CategoryController.java`

### 7.2 Tag Management

- **Entity**: `Tag.java`
- **DTO**: `TagCreateDTO.java`, `TagUpdateDTO.java`, `TagQueryDTO.java`
- **VO**: `TagVO.java`
- **Service**: `TagService.java`
- **Controller**: `TagController.java`

### 7.3 Comment Management

- **Entity**: `Comment.java`
- **DTO**: `CommentCreateDTO.java`, `CommentQueryDTO.java`
- **VO**: `CommentVO.java`
- **Service**: `CommentService.java`
- **Controller**: `CommentController.java`

### 7.4 Version Management

- **Entity**: `DocumentVersion.java`
- **DTO**: `DocumentVersionRestoreDTO.java`
- **VO**: `DocumentVersionVO.java`
- **Service**: `DocumentVersionService.java`
- **Controller**: `DocumentVersionController.java`

### 7.5 Review Management

- **Entity**: `DocumentReview.java`
- **DTO**: `DocumentReviewDTO.java`, `ReviewQueryDTO.java`
- **VO**: `DocumentReviewVO.java`
- **Service**: `DocumentReviewService.java`
- **Controller**: `DocumentReviewController.java`

---

## VIII. Testing and Verification

### 8.1 Run the Database Initialization Script

```bash
# Create the database
mysql -u root -p -e "CREATE DATABASE kb_document DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Run the table creation script
mysql -u root -p kb_document < sql/02_kb_document.sql

# Verify the tables were created successfully
mysql -u root -p kb_document -e "SHOW TABLES;"
```

### 8.2 Start the Service

```bash
# Option 1: start via Maven
cd kb-document
mvn spring-boot:run

# Option 2: start via IDE
# Open DocumentApplication.java in IDEA and click Run
```

Once started successfully, the console prints:

```
========================================
   Document service started successfully!
   Swagger doc URL: http://localhost:8082/api/document/doc.html
========================================
```

### 8.3 Access the API Docs

Open a browser and visit: `http://localhost:8082/api/document/doc.html`

### 8.4 Test the Endpoints

#### Create a Document

```bash
curl -X POST http://localhost:8082/api/document/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started with Spring Boot 3.x",
    "summary": "This article introduces the basics and a quick start guide for Spring Boot 3.x",
    "content": "Spring Boot 3.x is built on Spring Framework 6.x...",
    "documentType": 1,
    "categoryId": 1,
    "tags": "Spring Boot,Java,Backend Development",
    "status": 1,
    "allowComment": 1
  }'
```

#### Query the Document List

```bash
curl "http://localhost:8082/api/document/documents/page?current=1&size=10"
```

#### Query Document Details

```bash
curl "http://localhost:8082/api/document/documents/1"
```

#### Upload a File

```bash
curl -X POST http://localhost:8082/api/document/documents/upload \
  -F "file=@/path/to/document.pdf"
```

---

## IX. Summary

This article described in detail the process of building the document service module, covering the following core content:

### Completed Features

| Module | Feature | Description |
|------|------|------|
| Document management | CRUD, publish, archive, search | Complete document lifecycle management |
| Category management | Tree-structured categories, parent-child relationships | Flexible document categorization |
| Tag management | Tag CRUD, association management | Multi-dimensional document tagging |
| Comment management | Comments, replies, likes | User interaction |
| Version management | Version history, version comparison | Document version control |
| Review management | Review workflow, review records | Content quality assurance |
| File upload | Multi-format file support | Office, PDF, and other files |

### Technical Highlights

1. **Complete CRUD operations** - full document lifecycle management
2. **Rich text content support** - supports Markdown, rich text, and other formats
3. **File upload feature** - supports uploading multiple document formats
4. **Statistics feature** - tracks view count, like count, comment count, etc.
5. **Paginated queries** - supports combined multi-condition queries
6. **RESTful API** - standard REST endpoint design

### Future Improvements

1. Integrate Elasticsearch for full-text search
2. Add document export functionality (PDF, Word)
3. Implement collaborative document editing
4. Add document permission control
5. Implement a version comparison feature
6. Integrate AI-generated document summaries

Through this article, you should now be able to grasp:
- The design of a Spring Boot document management system
- Advanced MyBatis Plus query usage
- Implementing a file upload feature
- Handling tree-structured data
- Design patterns for a comment system

Happy building!
