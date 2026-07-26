-- =====================================================
-- kb_common database - common module
-- =====================================================

SET NAMES utf8mb4;
USE `kb_common`;
SET FOREIGN_KEY_CHECKS = 0;

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
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
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
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether default',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_dict_id` (`dict_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dictionary data table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_common database tables created!' AS message;
