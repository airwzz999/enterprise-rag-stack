-- =====================================================
-- kb_ai database - AI service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_ai`;
SET FOREIGN_KEY_CHECKS = 0;

-- AI conversation table
DROP TABLE IF EXISTS `kb_ai_conversation`;
CREATE TABLE `kb_ai_conversation` (
  `id` BIGINT NOT NULL COMMENT 'Conversation ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
  `title` VARCHAR(200) NOT NULL COMMENT 'Conversation title',
  `model_name` VARCHAR(50) DEFAULT 'qwen-turbo' COMMENT 'AI model name',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT 'Message count',
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
  `role` VARCHAR(20) NOT NULL COMMENT 'Role: user/assistant/system',
  `content` LONGTEXT NOT NULL COMMENT 'Message content',
  `tokens` INT DEFAULT NULL COMMENT 'Token count',
  `model_name` VARCHAR(50) DEFAULT NULL COMMENT 'Model used',
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
  `feedback_type` VARCHAR(20) NOT NULL COMMENT 'Feedback type: like/dislike',
  `comment` TEXT DEFAULT NULL COMMENT 'Feedback comment',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI feedback table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_ai database tables created!' AS message;
