-- =====================================================
-- kb_document database - document management service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_document`;
SET FOREIGN_KEY_CHECKS = 0;

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
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
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
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT 'Author name (redundant field)',
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
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`(100)),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
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
  UNIQUE KEY `uk_document_tag` (`document_id`, `tag_id`)
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
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT 'Author name',
  `is_current` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether the current version',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_version` (`document_id`, `version`),
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document version table';

-- Document comment table
DROP TABLE IF EXISTS `kb_comment`;
CREATE TABLE `kb_comment` (
  `id` BIGINT NOT NULL COMMENT 'Comment ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `content` TEXT NOT NULL COMMENT 'Comment content',
  `user_id` BIGINT NOT NULL COMMENT 'Commenting user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
  `user_avatar` VARCHAR(500) DEFAULT NULL COMMENT 'User avatar (redundant field)',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent comment ID',
  `reply_to_id` BIGINT DEFAULT NULL COMMENT 'ID of the comment being replied to',
  `reply_to_name` VARCHAR(50) DEFAULT NULL COMMENT 'Name of the recipient of the reply (redundant field)',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT 'Like count',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT 'Reply count',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document comment table';

-- Document review table
DROP TABLE IF EXISTS `kb_document_review`;
CREATE TABLE `kb_document_review` (
  `id` BIGINT NOT NULL COMMENT 'Review ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title (redundant field)',
  `submitter_id` BIGINT NOT NULL COMMENT 'Submitter ID',
  `submitter_name` VARCHAR(50) DEFAULT NULL COMMENT 'Submitter name (redundant field)',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT 'Reviewer ID',
  `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT 'Reviewer name (redundant field)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Status',
  `comment` TEXT DEFAULT NULL COMMENT 'Review comment',
  `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted at',
  `review_time` DATETIME DEFAULT NULL COMMENT 'Reviewed at',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_submitter_id` (`submitter_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document review table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_document database tables created!' AS message;
