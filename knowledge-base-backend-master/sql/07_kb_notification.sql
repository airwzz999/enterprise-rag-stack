-- =====================================================
-- kb_notification database - notification service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_notification`;
SET FOREIGN_KEY_CHECKS = 0;

-- System notification table
DROP TABLE IF EXISTS `kb_notification`;
CREATE TABLE `kb_notification` (
  `id` BIGINT NOT NULL COMMENT 'Notification ID',
  `user_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
  `notification_type` VARCHAR(20) NOT NULL COMMENT 'Notification type: system/comment/mention/review/like',
  `title` VARCHAR(200) NOT NULL COMMENT 'Notification title',
  `content` TEXT NOT NULL COMMENT 'Notification content',
  `link` VARCHAR(500) DEFAULT NULL COMMENT 'Jump link',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT 'Related type',
  `related_id` BIGINT DEFAULT NULL COMMENT 'Related ID',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether read',
  `read_time` DATETIME DEFAULT NULL COMMENT 'Read time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_create_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System notification table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_notification database tables created!' AS message;
