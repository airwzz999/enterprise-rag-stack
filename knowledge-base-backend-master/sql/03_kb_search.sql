-- =====================================================
-- kb_search database - search service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_search`;
SET FOREIGN_KEY_CHECKS = 0;

-- Search history table
DROP TABLE IF EXISTS `kb_search_history`;
CREATE TABLE `kb_search_history` (
  `id` BIGINT NOT NULL COMMENT 'Search history ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
  `keyword` VARCHAR(200) NOT NULL COMMENT 'Search keyword',
  `search_count` INT DEFAULT 0 COMMENT 'Search count',
  `search_type` VARCHAR(20) NOT NULL DEFAULT 'document' COMMENT 'Search type',
  `result_count` INT DEFAULT 0 COMMENT 'Result count',
  `search_params` JSON DEFAULT NULL COMMENT 'Search parameters',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Search time',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_keyword` (`keyword`(100)),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Search history table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_search database tables created!' AS message;
