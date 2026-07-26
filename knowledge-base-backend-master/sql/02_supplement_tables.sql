-- ========================================
-- Knowledge Base System supplementary table structures
-- Supports new features such as comments, review, versions, teams, files, etc.
-- ========================================

-- ========================================
-- Comment related tables
-- ========================================

-- Comment table
DROP TABLE IF EXISTS `tb_comment`;
CREATE TABLE `tb_comment` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Comment ID (Snowflake ID)',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',
    `parent_id` BIGINT(20) COMMENT 'Parent comment ID',
    `root_id` BIGINT(20) COMMENT 'Root comment ID',

    `content` TEXT NOT NULL COMMENT 'Comment content',

    `commenter_id` BIGINT(20) NOT NULL COMMENT 'Commenter ID',
    `commenter_name` VARCHAR(50) COMMENT 'Commenter name',
    `commenter_avatar` VARCHAR(500) COMMENT 'Commenter avatar',

    `reply_to_user_id` BIGINT(20) COMMENT 'Recipient of the reply (user ID)',
    `reply_to_user_name` VARCHAR(50) COMMENT 'Recipient of the reply (user name)',

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-hidden, 1-normal',

    `like_count` INT DEFAULT 0 COMMENT 'Like count',
    `reply_count` INT DEFAULT 0 COMMENT 'Reply count',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deletion flag',

    KEY `idx_document_id` (`document_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_root_id` (`root_id`),
    KEY `idx_commenter_id` (`commenter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comment table';

-- Like table
DROP TABLE IF EXISTS `tb_like`;
CREATE TABLE `tb_like` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Like ID',
    `target_id` BIGINT(20) NOT NULL COMMENT 'Target ID (document or comment)',
    `target_type` TINYINT NOT NULL COMMENT 'Target type: 1-document, 2-comment',
    `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',

    UNIQUE KEY `uk_target_user_type` (`target_id`, `user_id`, `target_type`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Like table';

-- Favorite table
DROP TABLE IF EXISTS `tb_favorite`;
CREATE TABLE `tb_favorite` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Favorite ID',
    `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',

    `folder_name` VARCHAR(100) COMMENT 'Favorites folder name',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deletion flag',

    UNIQUE KEY `uk_user_doc` (`user_id`, `document_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Favorite table';

-- View record table
DROP TABLE IF EXISTS `tb_view_record`;
CREATE TABLE `tb_view_record` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Record ID',
    `user_id` BIGINT(20) COMMENT 'User ID',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',

    `access_duration` INT DEFAULT 0 COMMENT 'Access duration (seconds)',
    `ip_address` VARCHAR(50) COMMENT 'IP address',
    `user_agent` VARCHAR(500) COMMENT 'User agent',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Access time',

    KEY `idx_user_id` (`user_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='View record table';

-- ========================================
-- Document review related tables
-- ========================================

-- Document review record table
DROP TABLE IF EXISTS `tb_document_review`;
CREATE TABLE `tb_document_review` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Review record ID (Snowflake ID)',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',

    `reviewer_id` BIGINT(20) NULL DEFAULT NULL COMMENT 'Reviewer ID (NULL when submitted for review, filled in when reviewed)',
    `reviewer_name` VARCHAR(50) COMMENT 'Reviewer name',

    `review_result` TINYINT NULL COMMENT 'Review result: NULL-pending, 1-approved, 2-rejected',
    `review_comment` TEXT COMMENT 'Review comment',

    `before_status` TINYINT COMMENT 'Status before review',

    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Reviewed at',

    `review_round` INT DEFAULT 1 COMMENT 'Review round',
    `review_level` INT DEFAULT 1 COMMENT 'Review level (1=first-level review, reserved for multi-level expansion)',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',

    KEY `idx_document_id` (`document_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    KEY `idx_reviewed_at` (`reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document review record table';

-- ========================================
-- Document version related tables
-- ========================================

-- Document version table
DROP TABLE IF EXISTS `tb_document_version`;
CREATE TABLE `tb_document_version` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Version ID (Snowflake ID)',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',
    `version` INT NOT NULL COMMENT 'Version number',

    `title` VARCHAR(200) COMMENT 'Document title',
    `content` LONGTEXT COMMENT 'Document content',
    `summary` VARCHAR(500) COMMENT 'Document summary',

    `change_description` VARCHAR(500) COMMENT 'Change description',
    `change_size` BIGINT COMMENT 'Change size (bytes)',

    `operator_id` BIGINT(20) COMMENT 'Operator ID',
    `operator_name` VARCHAR(50) COMMENT 'Operator name',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',

    UNIQUE KEY `uk_doc_version` (`document_id`, `version`),
    KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document version table';

-- ========================================
-- Team related tables
-- ========================================

-- Team table
DROP TABLE IF EXISTS `tb_team`;
CREATE TABLE `tb_team` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Team ID (Snowflake ID)',
    `team_name` VARCHAR(100) NOT NULL COMMENT 'Team name',
    `team_code` VARCHAR(50) NOT NULL COMMENT 'Team code',
    `description` VARCHAR(500) COMMENT 'Team description',

    `parent_id` BIGINT(20) COMMENT 'Parent team ID',
    `level` INT DEFAULT 1 COMMENT 'Team level',
    `path` VARCHAR(500) COMMENT 'Team path',

    `member_count` INT DEFAULT 0 COMMENT 'Member count',
    `doc_count` INT DEFAULT 0 COMMENT 'Document count',

    `leader_id` BIGINT(20) COMMENT 'Team leader ID',

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',

    `created_by` BIGINT(20) COMMENT 'Creator ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_by` BIGINT(20) COMMENT 'Updater ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deletion flag',

    UNIQUE KEY `uk_team_code` (`team_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_leader_id` (`leader_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team table';

-- Team member table
DROP TABLE IF EXISTS `tb_team_member`;
CREATE TABLE `tb_team_member` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Member ID',
    `team_id` BIGINT(20) NOT NULL COMMENT 'Team ID',
    `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',

    `member_role` TINYINT NOT NULL DEFAULT 2 COMMENT 'Member role: 0-leader, 1-admin, 2-regular member',

    `permissions` JSON COMMENT 'Team-level permission configuration',

    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Join time',

    `created_by` BIGINT(20) COMMENT 'Creator ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_by` BIGINT(20) COMMENT 'Updater ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deletion flag',

    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    KEY `idx_team_id` (`team_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Team member table';

-- ========================================
-- File related tables
-- ========================================

-- File table
DROP TABLE IF EXISTS `tb_file`;
CREATE TABLE `tb_file` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'File ID (Snowflake ID)',

    `original_name` VARCHAR(255) NOT NULL COMMENT 'Original file name',
    `stored_name` VARCHAR(255) NOT NULL COMMENT 'Stored file name',
    `file_path` VARCHAR(500) NOT NULL COMMENT 'File path',
    `file_size` BIGINT NOT NULL COMMENT 'File size (bytes)',

    `file_type` VARCHAR(20) NOT NULL COMMENT 'File type: DOCUMENT, IMAGE, VIDEO, AUDIO, OTHER',
    `mime_type` VARCHAR(100) NOT NULL COMMENT 'MIME type',

    `file_hash` VARCHAR(64) NOT NULL COMMENT 'File hash (SHA-256)',

    `storage_type` VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT 'Storage type',
    `bucket_name` VARCHAR(100) COMMENT 'Storage bucket name',

    `uploader_id` BIGINT(20) COMMENT 'Uploader ID',

    `access_level` TINYINT DEFAULT 0 COMMENT 'Access level: 0-private, 1-team visible, 2-public',

    `download_count` INT DEFAULT 0 COMMENT 'Download count',

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-deleted, 1-normal',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Deletion flag',

    UNIQUE KEY `uk_file_hash` (`file_hash`),
    KEY `idx_uploader_id` (`uploader_id`),
    KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File table';

-- ========================================
-- System configuration table
-- ========================================

-- System configuration table
DROP TABLE IF EXISTS `tb_system_config`;
CREATE TABLE `tb_system_config` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Config ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key',
    `config_value` TEXT COMMENT 'Config value',

    `config_group` VARCHAR(50) COMMENT 'Config group: AI, STORAGE, NOTIFICATION, SECURITY, etc.',
    `config_type` VARCHAR(20) COMMENT 'Config type: STRING, NUMBER, BOOLEAN, JSON',

    `description` VARCHAR(500) COMMENT 'Config description',

    `created_by` BIGINT(20) COMMENT 'Creator ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_by` BIGINT(20) COMMENT 'Updater ID',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',

    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System configuration table';

-- ========================================
-- Seed data
-- ========================================

-- Seed system configuration
INSERT INTO `tb_system_config` (`id`, `config_key`, `config_value`, `config_group`, `config_type`, `description`) VALUES
(9000000000000000001, 'qwen.api.key', '', 'AI', 'STRING', 'Qwen API key'),
(9000000000000000002, 'qwen.model.name', 'qwen-max', 'AI', 'STRING', 'Qwen model name'),
(9000000000000000003, 'qwen.embedding.model', 'text-embedding-v3', 'AI', 'STRING', 'Qwen embedding model'),
(9000000000000000004, 'milvus.host', 'localhost', 'AI', 'STRING', 'Milvus host address'),
(9000000000000000005, 'milvus.port', '19530', 'AI', 'NUMBER', 'Milvus port'),
(9000000000000000006, 'file.upload.max.size', '52428800', 'STORAGE', 'NUMBER', 'Maximum file upload size (bytes)'),
(9000000000000000007, 'file.upload.allowed.types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md', 'STORAGE', 'STRING', 'Allowed file upload types'),
(9000000000000000008, 'email.enabled', 'true', 'NOTIFICATION', 'BOOLEAN', 'Whether email notifications are enabled'),
(9000000000000000009, 'email.host', 'smtp.example.com', 'NOTIFICATION', 'STRING', 'Mail server address'),
(9000000000000000010, 'email.port', '587', 'NOTIFICATION', 'NUMBER', 'Mail server port');
