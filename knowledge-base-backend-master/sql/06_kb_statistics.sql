-- =====================================================
-- kb_statistics database - statistics service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_statistics`;
SET FOREIGN_KEY_CHECKS = 0;

-- Document statistics table
DROP TABLE IF EXISTS `kb_document_statistics`;
CREATE TABLE `kb_document_statistics` (
  `id` BIGINT NOT NULL COMMENT 'Statistics ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title (redundant field)',
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
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
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

-- View record table (deprecated, please use kb_view_history)
-- See: sql/13_kb_statistics_view_history.sql
-- DROP TABLE IF EXISTS `kb_view_record`;

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

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_statistics database tables created!' AS message;
