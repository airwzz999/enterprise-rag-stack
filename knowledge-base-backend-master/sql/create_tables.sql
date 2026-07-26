-- =====================================================
-- Enterprise Knowledge Base System - complete table creation script (DDL)
-- =====================================================
-- Version: 1.0
-- Database: MySQL 8.0+
-- Character set: utf8mb4
-- Collation: utf8mb4_unicode_ci
-- =====================================================
-- Execution notes:
--   1. Run as MySQL root or an account with CREATE DATABASE privileges
--   2. This script creates 10 microservice databases and their tables
--   3. The execution order is arranged according to dependencies and can be run in full
--   4. All CREATE TABLE statements use DROP TABLE IF EXISTS and can be re-run safely
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- Part 1: create databases
-- =====================================================

CREATE DATABASE IF NOT EXISTS `kb_user`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'User authentication service database';

CREATE DATABASE IF NOT EXISTS `kb_document`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Document management service database';

CREATE DATABASE IF NOT EXISTS `kb_search`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Search service database';

CREATE DATABASE IF NOT EXISTS `kb_file`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'File service database';

CREATE DATABASE IF NOT EXISTS `kb_ai`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'AI service database';

CREATE DATABASE IF NOT EXISTS `kb_statistics`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Statistics service database';

CREATE DATABASE IF NOT EXISTS `kb_notification`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Notification service database';

CREATE DATABASE IF NOT EXISTS `kb_graph`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Knowledge graph service database';

CREATE DATABASE IF NOT EXISTS `kb_common`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Common module database';

CREATE DATABASE IF NOT EXISTS `kb_foundation`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'Foundation service database (merges common + notification)';


-- =====================================================
-- Part 2: kb_user database - user authentication module
-- =====================================================

USE `kb_user`;

-- User table
DROP TABLE IF EXISTS `kb_user`;
CREATE TABLE `kb_user` (
  `id` BIGINT NOT NULL COMMENT 'User ID (Snowflake algorithm)',
  `username` VARCHAR(50) NOT NULL COMMENT 'Username',
  `password` VARCHAR(255) NOT NULL COMMENT 'Password (BCrypt encrypted)',
  `email` VARCHAR(100) NOT NULL COMMENT 'Email',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone number',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT 'Avatar URL',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'Real name',
  `department` VARCHAR(100) DEFAULT NULL COMMENT 'Department',
  `position` VARCHAR(100) DEFAULT NULL COMMENT 'Position',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Personal bio / remark',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `last_login_time` DATETIME DEFAULT NULL COMMENT 'Last login time',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT 'Last login IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag: 0-not deleted, 1-deleted',
  `tenant_id` BIGINT DEFAULT NULL COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`, `deleted`, `tenant_id`),
  UNIQUE KEY `uk_email` (`email`, `deleted`, `tenant_id`),
  KEY `idx_department` (`department`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- Role table
DROP TABLE IF EXISTS `kb_role`;
CREATE TABLE `kb_role` (
  `id` BIGINT NOT NULL COMMENT 'Role ID (Snowflake algorithm)',
  `role_name` VARCHAR(50) NOT NULL COMMENT 'Role name',
  `role_code` VARCHAR(50) NOT NULL COMMENT 'Role code',
  `description` VARCHAR(200) DEFAULT NULL COMMENT 'Role description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`, `deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role table';

-- Permission table
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL COMMENT 'Permission ID (Snowflake algorithm)',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent permission ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT 'Permission name',
  `permission_code` VARCHAR(100) NOT NULL COMMENT 'Permission code',
  `permission_type` TINYINT NOT NULL COMMENT 'Permission type: 1-menu, 2-button, 3-API',
  `menu_url` VARCHAR(200) DEFAULT NULL COMMENT 'Menu URL',
  `api_url` VARCHAR(500) DEFAULT NULL COMMENT 'API URL',
  `method` VARCHAR(10) DEFAULT NULL COMMENT 'Request method: GET,POST,PUT,DELETE',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Icon',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_permission_type` (`permission_type`),
  KEY `idx_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Permission table';

-- User-role association table
DROP TABLE IF EXISTS `kb_user_role`;
CREATE TABLE `kb_user_role` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `role_id` BIGINT NOT NULL COMMENT 'Role ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- Role-permission association table
DROP TABLE IF EXISTS `kb_role_permission`;
CREATE TABLE `kb_role_permission` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `role_id` BIGINT NOT NULL COMMENT 'Role ID',
  `permission_id` BIGINT NOT NULL COMMENT 'Permission ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-permission association table';

-- User-permission association table (permissions assigned directly to a user)
DROP TABLE IF EXISTS `kb_user_permission`;
CREATE TABLE `kb_user_permission` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `permission_id` BIGINT NOT NULL COMMENT 'Permission ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`user_id`, `permission_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-permission association table';

-- Team table
DROP TABLE IF EXISTS `kb_team`;
CREATE TABLE `kb_team` (
  `id` BIGINT NOT NULL COMMENT 'Team ID',
  `team_name` VARCHAR(100) NOT NULL COMMENT 'Team name',
  `team_code` VARCHAR(50) DEFAULT NULL COMMENT 'Team code',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Team description',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Team icon identifier',
  `leader_id` BIGINT DEFAULT NULL COMMENT 'Team leader ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent team ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_code` (`team_code`, `deleted`),
  KEY `idx_leader_id` (`leader_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team table';

-- Team member table
DROP TABLE IF EXISTS `kb_team_member`;
CREATE TABLE `kb_team_member` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `team_id` BIGINT NOT NULL COMMENT 'Team ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `member_role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'Member role: leader, member',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Join time',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Added by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team member table';

-- Token blacklist table
DROP TABLE IF EXISTS `tb_token_blacklist`;
CREATE TABLE `tb_token_blacklist` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `token_hash` VARCHAR(64) NOT NULL COMMENT 'Token hash value',
  `expire_time` DATETIME NOT NULL COMMENT 'Expiration time',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token blacklist table';


-- =====================================================
-- Part 3: kb_document database - document management module
-- =====================================================

USE `kb_document`;

-- Document category table
DROP TABLE IF EXISTS `kb_category`;
CREATE TABLE `kb_category` (
  `id` BIGINT NOT NULL COMMENT 'Category ID',
  `category_name` VARCHAR(50) NOT NULL COMMENT 'Category name',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent category ID',
  `category_icon` VARCHAR(50) DEFAULT 'tech' COMMENT 'Category icon identifier',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Category description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `document_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document category table';

-- Document tag table
DROP TABLE IF EXISTS `kb_tag`;
CREATE TABLE `kb_tag` (
  `id` BIGINT NOT NULL COMMENT 'Tag ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT 'Tag name',
  `tag_color` VARCHAR(20) DEFAULT '#1890ff' COMMENT 'Tag color',
  `description` VARCHAR(200) DEFAULT NULL COMMENT 'Tag description',
  `use_count` INT NOT NULL DEFAULT 0 COMMENT 'Use count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document tag table';

-- Document table
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL COMMENT 'Document ID',
  `title` VARCHAR(200) NOT NULL COMMENT 'Document title',
  `content` LONGTEXT NOT NULL COMMENT 'Document content',
  `summary` TEXT DEFAULT NULL COMMENT 'Document summary',
  `category_id` BIGINT DEFAULT NULL COMMENT 'Category ID',
  `team_id` BIGINT DEFAULT NULL COMMENT 'Team space ID',
  `author_id` BIGINT NOT NULL COMMENT 'Author ID',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT 'Author name',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT 'Cover image',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'Status: draft, published, archived',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether public: 0-private, 1-public',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether pinned: 0-no, 1-yes',
  `is_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether recommended: 0-no, 1-yes',
  `allow_comment` TINYINT NOT NULL DEFAULT 1 COMMENT 'Allow comments: 0-no, 1-yes',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT 'View count',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT 'Comment count',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT 'Favorite count',
  `version` INT NOT NULL DEFAULT 1 COMMENT 'Version number',
  `word_count` INT DEFAULT NULL COMMENT 'Word count',
  `document_type` TINYINT DEFAULT 1 COMMENT 'Document type: 1-article, 2-file',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT 'File path',
  `file_size` BIGINT DEFAULT NULL COMMENT 'File size (bytes)',
  `file_extension` VARCHAR(20) DEFAULT NULL COMMENT 'File extension',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
  `source` TINYINT DEFAULT 1 COMMENT 'Source: 1-original, 2-reposted, 3-translated',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT 'Source URL',
  `sort` INT DEFAULT 0 COMMENT 'Sort order',
  `publish_time` DATETIME DEFAULT NULL COMMENT 'Publish time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`(100)),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_create_time` (`created_at`),
  FULLTEXT KEY `ft_content` (`title`, `content`, `summary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document table';

-- Document-tag association table
DROP TABLE IF EXISTS `kb_document_tag`;
CREATE TABLE `kb_document_tag` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `tag_id` BIGINT NOT NULL COMMENT 'Tag ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_tag` (`document_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document-tag association table';

-- Document version table
DROP TABLE IF EXISTS `kb_document_version`;
CREATE TABLE `kb_document_version` (
  `id` BIGINT NOT NULL COMMENT 'Version ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `version` INT NOT NULL COMMENT 'Version number',
  `title` VARCHAR(200) NOT NULL COMMENT 'Document title',
  `content` LONGTEXT NOT NULL COMMENT 'Document content',
  `change_log` VARCHAR(500) DEFAULT NULL COMMENT 'Change description',
  `author_id` BIGINT NOT NULL COMMENT 'Author ID',
  `is_current` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether the current version',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version` (`document_id`, `version`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document version table';

-- Document comment table
DROP TABLE IF EXISTS `kb_comment`;
CREATE TABLE `kb_comment` (
  `id` BIGINT NOT NULL COMMENT 'Comment ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `content` TEXT NOT NULL COMMENT 'Comment content',
  `user_id` BIGINT NOT NULL COMMENT 'Commenting user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Commenting user name',
  `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT 'Commenting user avatar',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent comment ID',
  `reply_to_id` BIGINT DEFAULT NULL COMMENT 'ID of the comment being replied to',
  `reply_to_name` VARCHAR(50) DEFAULT NULL COMMENT 'Name of the user being replied to',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT 'Reply count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-hidden, 1-visible',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document comment table';

-- Document review table
DROP TABLE IF EXISTS `kb_document_review`;
CREATE TABLE `kb_document_review` (
  `id` BIGINT NOT NULL COMMENT 'Review ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `submitter_id` BIGINT NOT NULL COMMENT 'Submitter ID',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT 'Reviewer ID (NULL when submitted for review)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending, approved, rejected',
  `comment` TEXT DEFAULT NULL COMMENT 'Review comment',
  `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted at',
  `review_time` DATETIME DEFAULT NULL COMMENT 'Reviewed at',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_submitter_id` (`submitter_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document review table';

-- Document access record table
DROP TABLE IF EXISTS `kb_document_access`;
CREATE TABLE `kb_document_access` (
  `id` BIGINT NOT NULL COMMENT 'Access record ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title',
  `category_id` BIGINT DEFAULT NULL COMMENT 'Category ID',
  `category_name` VARCHAR(100) DEFAULT NULL COMMENT 'Category name',
  `access_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Access time',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'Access IP address',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `created_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `updated_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_document` (`user_id`, `document_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_access_time` (`access_time`),
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document access record table';

-- Document share table
DROP TABLE IF EXISTS `kb_document_share`;
CREATE TABLE `kb_document_share` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID (Snowflake algorithm)',
  `share_id` VARCHAR(32) NOT NULL COMMENT 'Share ID (unique identifier)',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `title` VARCHAR(255) DEFAULT NULL COMMENT 'Share title',
  `share_type` TINYINT DEFAULT 1 COMMENT 'Share type: 1-public link, 2-direct message share',
  `share_code` VARCHAR(32) DEFAULT NULL COMMENT 'Share code',
  `expire_type` TINYINT DEFAULT 1 COMMENT 'Expiration type: 1-permanent, 2-time-limited',
  `expire_time` DATETIME DEFAULT NULL COMMENT 'Expiration time',
  `access_limit` INT DEFAULT 0 COMMENT 'Access count limit (0-unlimited)',
  `access_count` INT DEFAULT 0 COMMENT 'Number of accesses so far',
  `require_password` TINYINT DEFAULT 0 COMMENT 'Whether a password is required: 0-no, 1-yes',
  `password` VARCHAR(64) DEFAULT NULL COMMENT 'Access password (MD5 encrypted)',
  `sharer_id` BIGINT DEFAULT NULL COMMENT 'Sharer ID',
  `sharer_name` VARCHAR(64) DEFAULT NULL COMMENT 'Sharer name',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Share description',
  `status` TINYINT DEFAULT 0 COMMENT 'Status: 0-active, 1-expired, 2-deleted',
  `share_time` DATETIME DEFAULT NULL COMMENT 'Share time',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_id` (`share_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_sharer_id` (`sharer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_share_time` (`share_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document share table';

-- User favorites table
DROP TABLE IF EXISTS `kb_user_favorite`;
CREATE TABLE `kb_user_favorite` (
  `id` BIGINT NOT NULL COMMENT 'Favorite ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title',
  `document_category_id` BIGINT DEFAULT NULL COMMENT 'Document category ID',
  `favorite_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Favorited at',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_document` (`user_id`, `document_id`, `deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_favorite_time` (`favorite_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User favorites table';


-- =====================================================
-- Part 4: kb_search database - search module
-- =====================================================

USE `kb_search`;

-- Search history table
DROP TABLE IF EXISTS `kb_search_history`;
CREATE TABLE `kb_search_history` (
  `id` BIGINT NOT NULL COMMENT 'Search history ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `keyword` VARCHAR(200) NOT NULL COMMENT 'Search keyword',
  `search_count` INT DEFAULT 0 COMMENT 'Search count',
  `search_type` VARCHAR(20) NOT NULL DEFAULT 'document' COMMENT 'Search type: document, user',
  `result_count` INT DEFAULT 0 COMMENT 'Result count',
  `search_params` JSON DEFAULT NULL COMMENT 'Search parameters',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Search time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_keyword` (`keyword`(100)),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Search history table';


-- =====================================================
-- Part 5: kb_file database - file management module
-- =====================================================

USE `kb_file`;

-- File information table
DROP TABLE IF EXISTS `kb_file`;
CREATE TABLE `kb_file` (
  `id` BIGINT NOT NULL COMMENT 'File ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT 'File name',
  `original_name` VARCHAR(255) NOT NULL COMMENT 'Original file name',
  `file_path` VARCHAR(500) NOT NULL COMMENT 'File path',
  `file_size` BIGINT NOT NULL COMMENT 'File size (bytes)',
  `file_type` VARCHAR(50) NOT NULL COMMENT 'File type',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
  `file_extension` VARCHAR(20) DEFAULT NULL COMMENT 'File extension',
  `storage_type` VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT 'Storage type: local, oss',
  `upload_user_id` BIGINT NOT NULL COMMENT 'Uploading user ID',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT 'Related type',
  `related_id` BIGINT DEFAULT NULL COMMENT 'Related ID',
  `download_count` INT NOT NULL DEFAULT 0 COMMENT 'Download count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_related` (`related_type`, `related_id`),
  KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File information table';


-- =====================================================
-- Part 6: kb_notification database - notification module
-- =====================================================

USE `kb_notification`;

-- System notification table
DROP TABLE IF EXISTS `kb_notification`;
CREATE TABLE `kb_notification` (
  `id` BIGINT NOT NULL COMMENT 'Notification ID',
  `user_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Recipient user name',
  `notification_type` VARCHAR(20) NOT NULL COMMENT 'Notification type: system, comment, mention, review, like',
  `title` VARCHAR(200) NOT NULL COMMENT 'Notification title',
  `content` TEXT NOT NULL COMMENT 'Notification content',
  `link` VARCHAR(500) DEFAULT NULL COMMENT 'Jump link',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether read: 0-unread, 1-read',
  `read_time` DATETIME DEFAULT NULL COMMENT 'Read time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System notification table';


-- =====================================================
-- Part 7: kb_ai database - AI module
-- =====================================================

USE `kb_ai`;

-- AI conversation table
DROP TABLE IF EXISTS `kb_ai_conversation`;
CREATE TABLE `kb_ai_conversation` (
  `id` BIGINT NOT NULL COMMENT 'Conversation ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name',
  `title` VARCHAR(200) NOT NULL COMMENT 'Conversation title',
  `model_name` VARCHAR(50) DEFAULT 'qwen' COMMENT 'AI model name',
  `message_count` INT DEFAULT 0 COMMENT 'Message count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_update_time` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI conversation table';

-- AI message table
DROP TABLE IF EXISTS `kb_ai_message`;
CREATE TABLE `kb_ai_message` (
  `id` BIGINT NOT NULL COMMENT 'Message ID',
  `conversation_id` BIGINT NOT NULL COMMENT 'Conversation ID',
  `role` VARCHAR(20) NOT NULL COMMENT 'Role: user, assistant, system',
  `content` LONGTEXT NOT NULL COMMENT 'Message content',
  `tokens` INT DEFAULT NULL COMMENT 'Token count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI message table';

-- AI feedback table
DROP TABLE IF EXISTS `kb_ai_feedback`;
CREATE TABLE `kb_ai_feedback` (
  `id` BIGINT NOT NULL COMMENT 'Feedback ID',
  `conversation_id` BIGINT NOT NULL COMMENT 'Conversation ID',
  `message_id` BIGINT NOT NULL COMMENT 'Message ID',
  `feedback_type` VARCHAR(20) NOT NULL COMMENT 'Feedback type: like, dislike',
  `comment` TEXT DEFAULT NULL COMMENT 'Feedback comment',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI feedback table';


-- =====================================================
-- Part 8: kb_statistics database - statistics module
-- =====================================================

USE `kb_statistics`;

-- Document statistics table
DROP TABLE IF EXISTS `kb_document_statistics`;
CREATE TABLE `kb_document_statistics` (
  `id` BIGINT NOT NULL COMMENT 'Statistics ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT 'View count',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT 'Comment count',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT 'Favorite count',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT 'Share count',
  `stat_date` DATE NOT NULL COMMENT 'Statistics date',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_date` (`document_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document statistics table';

-- User statistics table
DROP TABLE IF EXISTS `kb_user_statistics`;
CREATE TABLE `kb_user_statistics` (
  `id` BIGINT NOT NULL COMMENT 'Statistics ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name',
  `document_count` INT NOT NULL DEFAULT 0 COMMENT 'Document count',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT 'Comment count',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT 'View count',
  `login_count` INT NOT NULL DEFAULT 0 COMMENT 'Login count',
  `stat_date` DATE NOT NULL COMMENT 'Statistics date',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User statistics table';

-- View record table (old)
DROP TABLE IF EXISTS `kb_view_record`;
CREATE TABLE `kb_view_record` (
  `id` BIGINT NOT NULL COMMENT 'Record ID',
  `user_id` BIGINT DEFAULT NULL COMMENT 'User ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `view_duration` INT DEFAULT NULL COMMENT 'View duration (seconds)',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP address',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'View time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='View record table';

-- View history table (new)
DROP TABLE IF EXISTS `kb_view_history`;
CREATE TABLE `kb_view_history` (
  `id` BIGINT NOT NULL COMMENT 'Record ID (Snowflake algorithm generated)',
  `user_id` BIGINT DEFAULT NULL COMMENT 'User ID (NULL if not logged in)',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title',
  `view_duration` INT DEFAULT NULL COMMENT 'View duration (seconds)',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP address',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'View time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_create_time` (`created_at`),
  KEY `idx_user_document` (`user_id`, `document_id`),
  KEY `idx_doc_date` (`document_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User view history table';

-- Comment statistics table
DROP TABLE IF EXISTS `kb_comment_statistics`;
CREATE TABLE `kb_comment_statistics` (
  `id` BIGINT NOT NULL COMMENT 'Statistics ID',
  `comment_id` BIGINT NOT NULL COMMENT 'Comment ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT 'Reply count',
  `stat_date` DATE NOT NULL COMMENT 'Statistics date',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_date` (`comment_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comment statistics table';


-- =====================================================
-- Part 9: kb_common database - common module
-- =====================================================

USE `kb_common`;

-- Operation log table
DROP TABLE IF EXISTS `kb_operation_log`;
CREATE TABLE `kb_operation_log` (
  `id` BIGINT NOT NULL COMMENT 'Log ID',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module name',
  `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation type',
  `operation_desc` VARCHAR(500) NOT NULL COMMENT 'Operation description',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request method',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
  `request_params` TEXT DEFAULT NULL COMMENT 'Request parameters',
  `response_result` TEXT DEFAULT NULL COMMENT 'Response result',
  `user_id` BIGINT DEFAULT NULL COMMENT 'Operating user ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT 'Operating username',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP address',
  `location` VARCHAR(200) DEFAULT NULL COMMENT 'Geographic location',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `execute_time` INT DEFAULT NULL COMMENT 'Execution duration (milliseconds)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-failure, 1-success',
  `error_msg` TEXT DEFAULT NULL COMMENT 'Error message',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation log table';

-- System configuration table
DROP TABLE IF EXISTS `kb_system_config`;
CREATE TABLE `kb_system_config` (
  `id` BIGINT NOT NULL COMMENT 'Config ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key',
  `config_value` TEXT NOT NULL COMMENT 'Config value',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT 'Config type: string, number, boolean, json',
  `category` VARCHAR(50) DEFAULT NULL COMMENT 'Config category',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Config description',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether public: 0-no, 1-yes',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System configuration table';

-- Dictionary table
DROP TABLE IF EXISTS `kb_dict`;
CREATE TABLE `kb_dict` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary ID',
  `dict_code` VARCHAR(50) NOT NULL COMMENT 'Dictionary code',
  `dict_name` VARCHAR(100) NOT NULL COMMENT 'Dictionary name',
  `dict_type` VARCHAR(50) NOT NULL COMMENT 'Dictionary type',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary table';

-- Dictionary data table
DROP TABLE IF EXISTS `kb_dict_data`;
CREATE TABLE `kb_dict_data` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary data ID',
  `dict_id` BIGINT NOT NULL COMMENT 'Dictionary ID',
  `dict_label` VARCHAR(100) NOT NULL COMMENT 'Dictionary label',
  `dict_value` VARCHAR(200) NOT NULL COMMENT 'Dictionary value',
  `dict_sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT 'CSS class name',
  `list_class` VARCHAR(100) DEFAULT NULL COMMENT 'List style',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether default: 0-no, 1-yes',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_dict_id` (`dict_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary data table';


-- =====================================================
-- Part 10: kb_foundation database - foundation service
-- =====================================================

USE `kb_foundation`;

-- System configuration table (foundation service)
DROP TABLE IF EXISTS `kb_system_config`;
CREATE TABLE `kb_system_config` (
  `id` BIGINT NOT NULL COMMENT 'Config ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key',
  `config_value` TEXT NOT NULL COMMENT 'Config value',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT 'Config type',
  `category` VARCHAR(50) DEFAULT NULL COMMENT 'Config category',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Config description',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether public',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System configuration table';

-- Dictionary table
DROP TABLE IF EXISTS `kb_dict`;
CREATE TABLE `kb_dict` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary ID',
  `dict_code` VARCHAR(50) NOT NULL COMMENT 'Dictionary code',
  `dict_name` VARCHAR(100) NOT NULL COMMENT 'Dictionary name',
  `dict_type` VARCHAR(50) NOT NULL COMMENT 'Dictionary type',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary table';

-- Dictionary data table
DROP TABLE IF EXISTS `kb_dict_data`;
CREATE TABLE `kb_dict_data` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary data ID',
  `dict_id` BIGINT NOT NULL COMMENT 'Dictionary ID',
  `dict_code` VARCHAR(50) DEFAULT NULL COMMENT 'Dictionary code (redundant)',
  `dict_label` VARCHAR(100) NOT NULL COMMENT 'Dictionary label',
  `dict_value` VARCHAR(200) NOT NULL COMMENT 'Dictionary value',
  `dict_sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT 'CSS class name',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether default',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_dict_id` (`dict_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary data table';

-- System notification table
DROP TABLE IF EXISTS `kb_notification`;
CREATE TABLE `kb_notification` (
  `id` BIGINT NOT NULL COMMENT 'Notification ID',
  `user_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Recipient user name',
  `notification_type` VARCHAR(20) NOT NULL COMMENT 'Notification type',
  `title` VARCHAR(200) NOT NULL COMMENT 'Notification title',
  `content` TEXT NOT NULL COMMENT 'Notification content',
  `link` VARCHAR(500) DEFAULT NULL COMMENT 'Jump link',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether read',
  `read_time` DATETIME DEFAULT NULL COMMENT 'Read time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System notification table';

-- Operation log table
DROP TABLE IF EXISTS `kb_operation_log`;
CREATE TABLE `kb_operation_log` (
  `id` BIGINT NOT NULL COMMENT 'Log ID',
  `module` VARCHAR(100) DEFAULT NULL COMMENT 'Operation module',
  `operation_type` VARCHAR(50) DEFAULT NULL COMMENT 'Operation type',
  `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT 'Operation description',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request method',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
  `request_params` TEXT DEFAULT NULL COMMENT 'Request parameters',
  `response_result` TEXT DEFAULT NULL COMMENT 'Response result',
  `user_id` BIGINT DEFAULT NULL COMMENT 'Operating user ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT 'Operating username',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP address',
  `location` VARCHAR(200) DEFAULT NULL COMMENT 'Geographic location',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `execute_time` INT DEFAULT NULL COMMENT 'Execution duration (milliseconds)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `error_msg` TEXT DEFAULT NULL COMMENT 'Error message',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation log table';

-- Notification template table
DROP TABLE IF EXISTS `kb_notification_template`;
CREATE TABLE `kb_notification_template` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `template_code` VARCHAR(100) NOT NULL COMMENT 'Template code',
  `template_name` VARCHAR(200) NOT NULL COMMENT 'Template name',
  `notification_type` VARCHAR(50) NOT NULL COMMENT 'Notification type: EMAIL/SMS/WECHAT/SYSTEM/BROWSER',
  `title` VARCHAR(500) NOT NULL COMMENT 'Template title',
  `content` TEXT NOT NULL COMMENT 'Template content',
  `variables` VARCHAR(1000) DEFAULT '[]' COMMENT 'Template variables (JSON array format)',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Template description',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT 'Whether enabled: 0-disabled, 1-enabled',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification template table';


-- =====================================================
-- Part 11: kb_graph database - knowledge graph module
-- =====================================================

USE `kb_graph`;

-- Graph node table
DROP TABLE IF EXISTS `kb_graph_node`;
CREATE TABLE `kb_graph_node` (
  `id` BIGINT NOT NULL COMMENT 'Node ID',
  `node_name` VARCHAR(200) NOT NULL COMMENT 'Node name',
  `node_type` VARCHAR(50) NOT NULL COMMENT 'Node type: document, tag, user',
  `source_id` BIGINT DEFAULT NULL COMMENT 'Source data ID',
  `properties` JSON DEFAULT NULL COMMENT 'Node properties',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node` (`node_type`, `source_id`),
  KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Graph node table';

-- Graph relationship table
DROP TABLE IF EXISTS `kb_graph_edge`;
CREATE TABLE `kb_graph_edge` (
  `id` BIGINT NOT NULL COMMENT 'Relationship ID',
  `source_node_id` BIGINT NOT NULL COMMENT 'Source node ID',
  `target_node_id` BIGINT NOT NULL COMMENT 'Target node ID',
  `relation_type` VARCHAR(50) NOT NULL COMMENT 'Relationship type: similar, related, reference',
  `weight` FLOAT DEFAULT 1.0 COMMENT 'Relationship weight',
  `properties` JSON DEFAULT NULL COMMENT 'Relationship properties',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_source_node` (`source_node_id`),
  KEY `idx_target_node` (`target_node_id`),
  KEY `idx_relation_type` (`relation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Graph relationship table';


-- =====================================================
-- Part 12: cross-database views
-- =====================================================

-- Cross-database view in the kb_user database
USE `kb_user`;
DROP VIEW IF EXISTS `kb_document`;
CREATE VIEW `kb_document` AS
SELECT
    id, title, author_id, author_name, category_id, status,
    view_count, like_count, favorite_count, comment_count,
    is_public, is_top, is_recommend, document_type, source,
    cover_image, tags, summary,
    sort, allow_comment, publish_time, created_at, updated_at,
    create_by, update_by, deleted
FROM kb_document.kb_document;

-- Cross-database views in the kb_statistics database
USE `kb_statistics`;

DROP VIEW IF EXISTS `kb_document`;
CREATE VIEW `kb_document` AS
SELECT
    id, title, author_id, author_name, category_id, status,
    view_count, like_count, favorite_count, comment_count,
    is_public, is_top, is_recommend, document_type, source,
    cover_image, tags, summary,
    sort, allow_comment, publish_time, created_at, updated_at,
    create_by, update_by, deleted
FROM kb_document.kb_document;

DROP VIEW IF EXISTS `kb_user`;
CREATE VIEW `kb_user` AS
SELECT
    id, username, real_name, avatar, status,
    email, phone, department, position,
    last_login_time, created_at, updated_at, deleted
FROM kb_user.kb_user;

DROP VIEW IF EXISTS `kb_comment`;
CREATE VIEW `kb_comment` AS
SELECT
    id, document_id, content, user_id, user_name,
    user_avatar, parent_id, reply_to_id, reply_to_name,
    like_count, reply_count, status, created_at,
    updated_at, deleted
FROM kb_document.kb_comment;

DROP VIEW IF EXISTS `kb_operation_log`;
CREATE VIEW `kb_operation_log` AS
SELECT
    id, module, operation_type, operation_desc,
    request_method, request_url, request_params,
    response_result, user_id, username, ip_address,
    location, user_agent, execute_time, status,
    error_msg, created_at
FROM kb_foundation.kb_operation_log;

DROP VIEW IF EXISTS `kb_category`;
CREATE VIEW `kb_category` AS
SELECT
    id, category_name, parent_id, category_icon,
    description, sort, document_count, status,
    created_at, updated_at, create_by, update_by, deleted
FROM kb_document.kb_category;

DROP VIEW IF EXISTS `kb_ai_conversation`;
CREATE VIEW `kb_ai_conversation` AS
SELECT
    id, title, user_id, model, system_prompt,
    tokens_used, message_count, status,
    created_at, updated_at, deleted
FROM kb_ai.conversation;

DROP VIEW IF EXISTS `kb_ai_message`;
CREATE VIEW `kb_ai_message` AS
SELECT
    id, conversation_id, role, content, tokens,
    created_at, deleted
FROM kb_ai.message;


-- =====================================================
-- Done
-- =====================================================

SET FOREIGN_KEY_CHECKS = 1;

SELECT '========================================' AS '';
SELECT '  Database table structure creation complete!' AS message;
SELECT '========================================' AS '';
SELECT CONCAT('Total databases: ', 10) AS summary;
SELECT CONCAT('Total tables:    ', 30) AS summary;
