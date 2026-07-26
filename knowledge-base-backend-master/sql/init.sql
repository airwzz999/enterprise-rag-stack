-- ========================================
-- Knowledge Base System database initialization script
-- ========================================

-- Create the database
CREATE DATABASE IF NOT EXISTS `knowledge_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `knowledge_base`;

-- ========================================
-- User authentication related tables
-- ========================================

-- User table
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL COMMENT 'User ID (Snowflake algorithm)',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',
    `password` VARCHAR(255) NOT NULL COMMENT 'Password (encrypted)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT 'Nickname',
    `email` VARCHAR(100) DEFAULT NULL COMMENT 'Email',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone number',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT 'Avatar URL',
    `gender` TINYINT DEFAULT 0 COMMENT 'Gender (0-unknown, 1-male, 2-female)',
    `status` TINYINT DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    `dept_id` BIGINT DEFAULT NULL COMMENT 'Department ID',
    `post_id` BIGINT DEFAULT NULL COMMENT 'Position ID',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion (0-not deleted, 1-deleted)',
    `version` INT DEFAULT 0 COMMENT 'Optimistic lock version number',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- Role table
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL COMMENT 'Role ID (Snowflake algorithm)',
    `role_name` VARCHAR(50) NOT NULL COMMENT 'Role name',
    `role_code` VARCHAR(50) NOT NULL COMMENT 'Role code',
    `description` VARCHAR(200) DEFAULT NULL COMMENT 'Role description',
    `sort` INT DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion (0-not deleted, 1-deleted)',
    `version` INT DEFAULT 0 COMMENT 'Optimistic lock version number',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role table';

-- Permission table
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` BIGINT NOT NULL COMMENT 'Permission ID (Snowflake algorithm)',
    `parent_id` BIGINT DEFAULT 0 COMMENT 'Parent permission ID',
    `permission_name` VARCHAR(50) NOT NULL COMMENT 'Permission name',
    `permission_code` VARCHAR(100) NOT NULL COMMENT 'Permission code',
    `permission_type` TINYINT DEFAULT 1 COMMENT 'Permission type (1-menu, 2-button)',
    `path` VARCHAR(200) DEFAULT NULL COMMENT 'Permission path',
    `component` VARCHAR(200) DEFAULT NULL COMMENT 'Component path',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT 'Icon',
    `sort` INT DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion (0-not deleted, 1-deleted)',
    `version` INT DEFAULT 0 COMMENT 'Optimistic lock version number',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Permission table';

-- User-role association table
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL COMMENT 'Primary key ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `role_id` BIGINT NOT NULL COMMENT 'Role ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- Role-permission association table
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` BIGINT NOT NULL COMMENT 'Primary key ID',
    `role_id` BIGINT NOT NULL COMMENT 'Role ID',
    `permission_id` BIGINT NOT NULL COMMENT 'Permission ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-permission association table';

-- ========================================
-- Document related tables
-- ========================================

-- Document category table
DROP TABLE IF EXISTS `kb_category`;
CREATE TABLE `kb_category` (
    `id` BIGINT NOT NULL COMMENT 'Category ID (Snowflake algorithm)',
    `parent_id` BIGINT DEFAULT 0 COMMENT 'Parent category ID (0 means root category)',
    `category_name` VARCHAR(50) NOT NULL COMMENT 'Category name',
    `category_code` VARCHAR(50) NOT NULL COMMENT 'Category code',
    `description` VARCHAR(200) DEFAULT NULL COMMENT 'Category description',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT 'Icon',
    `sort` INT DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
    `document_count` INT DEFAULT 0 COMMENT 'Document count',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion (0-not deleted, 1-deleted)',
    `version` INT DEFAULT 0 COMMENT 'Optimistic lock version number',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document category table';

-- Document table
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
    `id` BIGINT NOT NULL COMMENT 'Document ID (Snowflake algorithm)',
    `title` VARCHAR(200) NOT NULL COMMENT 'Document title',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT 'Document summary',
    `content` LONGTEXT COMMENT 'Document content',
    `document_type` TINYINT DEFAULT 1 COMMENT 'Document type (1-article, 2-file)',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'File path',
    `file_size` BIGINT DEFAULT NULL COMMENT 'File size (bytes)',
    `file_extension` VARCHAR(20) DEFAULT NULL COMMENT 'File extension',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
    `category_id` BIGINT DEFAULT NULL COMMENT 'Category ID',
    `tags` VARCHAR(200) DEFAULT NULL COMMENT 'Tags (comma-separated)',
    `status` TINYINT DEFAULT 0 COMMENT 'Status (0-draft, 1-published, 2-archived)',
    `is_top` TINYINT DEFAULT 0 COMMENT 'Whether pinned (0-no, 1-yes)',
    `is_recommend` TINYINT DEFAULT 0 COMMENT 'Whether recommended (0-no, 1-yes)',
    `view_count` BIGINT DEFAULT 0 COMMENT 'View count',
    `like_count` BIGINT DEFAULT 0 COMMENT 'Like count',
    `favorite_count` BIGINT DEFAULT 0 COMMENT 'Favorite count',
    `comment_count` BIGINT DEFAULT 0 COMMENT 'Comment count',
    `publish_time` DATETIME DEFAULT NULL COMMENT 'Publish time',
    `author_id` BIGINT DEFAULT NULL COMMENT 'Author ID',
    `author_name` VARCHAR(50) DEFAULT NULL COMMENT 'Author name',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT 'Cover image URL',
    `source` TINYINT DEFAULT 1 COMMENT 'Source (1-original, 2-reposted, 3-translated)',
    `source_url` VARCHAR(500) DEFAULT NULL COMMENT 'Source URL',
    `allow_comment` TINYINT DEFAULT 1 COMMENT 'Allow comments (0-no, 1-yes)',
    `sort` INT DEFAULT 0 COMMENT 'Sort order',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion (0-not deleted, 1-deleted)',
    `version` INT DEFAULT 0 COMMENT 'Optimistic lock version number',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_status` (`status`),
    KEY `idx_publish_time` (`publish_time`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document table';

-- Document tag table
DROP TABLE IF EXISTS `kb_tag`;
CREATE TABLE `kb_tag` (
    `id` BIGINT NOT NULL COMMENT 'Tag ID (Snowflake algorithm)',
    `tag_name` VARCHAR(50) NOT NULL COMMENT 'Tag name',
    `tag_count` INT DEFAULT 0 COMMENT 'Document count',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document tag table';

-- ========================================
-- Seed data
-- ========================================

-- Insert the default admin user (password: 123456)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`)
VALUES (1234567890123456789, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'System Administrator', 'admin@knowledge-base.com', 1);

-- Insert default roles
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `sort`)
VALUES
(1234567890123456790, 'Super Admin', 'ROLE_ADMIN', 'System super administrator, has all permissions', 1),
(1234567890123456791, 'Regular User', 'ROLE_USER', 'Regular system user', 2);

-- Insert the role association for the admin user
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`)
VALUES (1234567890123456792, 1234567890123456789, 1234567890123456790);

-- Insert default document categories
INSERT INTO `kb_category` (`id`, `parent_id`, `category_name`, `category_code`, `description`, `sort`)
VALUES
(1234567890123456793, 0, 'Technical Documentation', 'TECH', 'Technology-related documentation', 1),
(1234567890123456794, 0, 'Product Documentation', 'PRODUCT', 'Product-related documentation', 2),
(1234567890123456795, 0, 'Operations Documentation', 'OPERATION', 'Operations-related documentation', 3);

-- ========================================
-- Create views
-- ========================================

-- Document list view
CREATE OR REPLACE VIEW `v_document_list` AS
SELECT
    d.id,
    d.title,
    d.summary,
    d.document_type,
    d.status,
    d.view_count,
    d.like_count,
    d.comment_count,
    d.publish_time,
    d.author_id,
    d.author_name,
    d.cover_image,
    d.created_at,
    c.category_name,
    c.id as category_id
FROM kb_document d
LEFT JOIN kb_category c ON d.category_id = c.id
WHERE d.deleted = 0;

-- ========================================
-- Index optimization
-- ========================================

-- Composite index on the document table
CREATE INDEX idx_doc_status_top_sort ON kb_document(status, is_top, sort, publish_time);
CREATE INDEX idx_doc_category_status ON kb_document(category_id, status);

-- Composite index on the user table
CREATE INDEX idx_user_status_deleted ON sys_user(status, deleted);
