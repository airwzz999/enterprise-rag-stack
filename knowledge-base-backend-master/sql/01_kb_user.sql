-- =====================================================
-- kb_user database - user authentication service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;
SET FOREIGN_KEY_CHECKS = 0;

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
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-enabled',
  `last_login_time` DATETIME DEFAULT NULL COMMENT 'Last login time',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT 'Last login IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag: 0-not deleted, 1-deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`, `deleted`),
  UNIQUE KEY `uk_email` (`email`, `deleted`),
  KEY `idx_department` (`department`),
  KEY `idx_status` (`status`)
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
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role table';

-- Permission table
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL COMMENT 'Permission ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent permission ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT 'Permission name',
  `permission_code` VARCHAR(100) NOT NULL COMMENT 'Permission code',
  `permission_type` TINYINT NOT NULL COMMENT 'Permission type: 1-menu, 2-button, 3-API',
  `menu_url` VARCHAR(200) DEFAULT NULL COMMENT 'Menu URL',
  `api_url` VARCHAR(500) DEFAULT NULL COMMENT 'API URL',
  `method` VARCHAR(10) DEFAULT NULL COMMENT 'Request method',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Icon',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_permission_type` (`permission_type`)
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
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- Role-permission association table
DROP TABLE IF EXISTS `kb_role_permission`;
CREATE TABLE `kb_role_permission` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `role_id` BIGINT NOT NULL COMMENT 'Role ID',
  `permission_id` BIGINT NOT NULL COMMENT 'Permission ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-permission association table';

-- User-permission association table (permissions assigned directly to a user)
DROP TABLE IF EXISTS `kb_user_permission`;
CREATE TABLE `kb_user_permission` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `permission_id` BIGINT NOT NULL COMMENT 'Permission ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`user_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-permission association table';

-- Team table
DROP TABLE IF EXISTS `kb_team`;
CREATE TABLE `kb_team` (
  `id` BIGINT NOT NULL COMMENT 'Team ID',
  `team_name` VARCHAR(100) NOT NULL COMMENT 'Team name',
  `team_code` VARCHAR(50) DEFAULT NULL COMMENT 'Team code',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'Team description',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Team icon (emoji)',
  `leader_id` BIGINT DEFAULT NULL COMMENT 'Team leader ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent team ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Creator',
  `update_by` BIGINT DEFAULT NULL COMMENT 'Updater',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_code` (`team_code`, `deleted`),
  KEY `idx_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team table';

-- Team member table
DROP TABLE IF EXISTS `kb_team_member`;
CREATE TABLE `kb_team_member` (
  `id` BIGINT NOT NULL COMMENT 'Primary key ID',
  `team_id` BIGINT NOT NULL COMMENT 'Team ID',
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `member_role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'Member role',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Join time',
  `create_by` BIGINT DEFAULT NULL COMMENT 'Added by',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team member table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_user database tables created!' AS message;
