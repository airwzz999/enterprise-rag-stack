-- =====================================================
-- kb_foundation database - foundation service (merges kb_common and kb_notification)
-- =====================================================

SET NAMES utf8mb4;
USE `kb_foundation`;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. System notification table
-- =====================================================
DROP TABLE IF EXISTS `kb_notification`;
CREATE TABLE `kb_notification` (
  `id` BIGINT NOT NULL COMMENT 'Notification ID (Snowflake ID)',
  `user_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT 'User name (redundant field)',
  `notification_type` VARCHAR(20) NOT NULL COMMENT 'Notification type: system/comment/mention/review/like',
  `title` VARCHAR(200) NOT NULL COMMENT 'Notification title',
  `content` TEXT NOT NULL COMMENT 'Notification content',
  `link` VARCHAR(500) DEFAULT NULL COMMENT 'Jump link',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT 'Related type',
  `related_id` BIGINT DEFAULT NULL COMMENT 'Related ID',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether read: 0-unread, 1-read',
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
  KEY `idx_create_time` (`created_at`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System notification table';

-- =====================================================
-- 2. System configuration table
-- =====================================================
DROP TABLE IF EXISTS `kb_system_config`;
CREATE TABLE `kb_system_config` (
  `id` BIGINT NOT NULL COMMENT 'Config ID (Snowflake ID)',
  `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key',
  `config_value` TEXT NOT NULL COMMENT 'Config value',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT 'Config type: string/number/boolean/json',
  `category` VARCHAR(50) DEFAULT NULL COMMENT 'Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Config description',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether public: 0-private, 1-public',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System configuration table';

-- =====================================================
-- 3. Operation log table
-- =====================================================
DROP TABLE IF EXISTS `kb_operation_log`;
CREATE TABLE `kb_operation_log` (
  `id` BIGINT NOT NULL COMMENT 'Log ID (Snowflake ID)',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module name',
  `operation_type` VARCHAR(50) NOT NULL COMMENT 'Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.',
  `operation_desc` VARCHAR(500) NOT NULL COMMENT 'Operation description',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'Request method: GET/POST/PUT/DELETE',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT 'Request URL',
  `request_params` TEXT DEFAULT NULL COMMENT 'Request parameters (JSON)',
  `response_result` TEXT DEFAULT NULL COMMENT 'Response result (JSON)',
  `user_id` BIGINT DEFAULT NULL COMMENT 'Operating user ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT 'Operating username',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP address',
  `location` VARCHAR(200) DEFAULT NULL COMMENT 'Geographic location',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User agent',
  `execute_time` INT DEFAULT NULL COMMENT 'Execution duration (milliseconds)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-failure, 1-success',
  `error_msg` TEXT DEFAULT NULL COMMENT 'Error message',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`created_at`),
  KEY `idx_status` (`status`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation log table';

-- =====================================================
-- 4. Dictionary type table
-- =====================================================
DROP TABLE IF EXISTS `kb_dict`;
CREATE TABLE `kb_dict` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary ID (Snowflake ID)',
  `dict_code` VARCHAR(50) NOT NULL COMMENT 'Dictionary code',
  `dict_name` VARCHAR(100) NOT NULL COMMENT 'Dictionary name',
  `dict_type` VARCHAR(50) NOT NULL COMMENT 'Dictionary type',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-active',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary type table';

-- =====================================================
-- 5. Dictionary data table
-- =====================================================
DROP TABLE IF EXISTS `kb_dict_data`;
CREATE TABLE `kb_dict_data` (
  `id` BIGINT NOT NULL COMMENT 'Dictionary data ID (Snowflake ID)',
  `dict_id` BIGINT NOT NULL COMMENT 'Dictionary ID',
  `dict_code` VARCHAR(50) NOT NULL COMMENT 'Dictionary code (redundant)',
  `dict_label` VARCHAR(100) NOT NULL COMMENT 'Dictionary label',
  `dict_value` VARCHAR(200) NOT NULL COMMENT 'Dictionary value',
  `dict_sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT 'CSS class name',
  `list_class` VARCHAR(100) DEFAULT NULL COMMENT 'List style',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether default: 0-no, 1-yes',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-active',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  KEY `idx_dict_id` (`dict_id`),
  KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary data table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_foundation database tables created!' AS message;
SHOW TABLES;
