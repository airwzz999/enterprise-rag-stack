-- =====================================================
-- kb_favorite database - favorites feature
-- =====================================================

SET NAMES utf8mb4;
USE `kb_document`;
SET FOREIGN_KEY_CHECKS = 0;

-- User favorites table
DROP TABLE IF EXISTS `kb_user_favorite`;
CREATE TABLE `kb_user_favorite` (
  `id` BIGINT NOT NULL COMMENT 'Favorite ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `document_id` BIGINT NOT NULL COMMENT 'Document ID',
  `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title (redundant field)',
  `document_category_id` BIGINT DEFAULT NULL COMMENT 'Document category ID (redundant field)',
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

SELECT 'kb_favorite table created!' AS message;
