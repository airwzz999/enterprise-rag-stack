-- =====================================================
-- kb_statistics database - view history table
-- Replaces the original kb_view_record, with added index optimizations
-- =====================================================

SET NAMES utf8mb4;
USE `kb_statistics`;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop the old table (if it exists)
DROP TABLE IF EXISTS `kb_view_record`;

-- Create the view history table
CREATE TABLE `kb_view_history` (
    `id` BIGINT NOT NULL COMMENT 'Record ID (Snowflake algorithm generated)',
    `user_id` BIGINT DEFAULT NULL COMMENT 'User ID (NULL if not logged in)',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field, speeds up queries)',
    `document_id` BIGINT NOT NULL COMMENT 'Document ID',
    `document_title` VARCHAR(200) DEFAULT NULL COMMENT 'Document title (redundant field, speeds up queries)',
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

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_view_history table created!' AS message;
