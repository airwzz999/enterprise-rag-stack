-- Document share table
-- Stores document sharing information, including share links, expiration, and access permissions

CREATE TABLE IF NOT EXISTS `kb_document_share` (
    `id` BIGINT NOT NULL COMMENT 'Primary key ID (Snowflake algorithm)',
    `share_id` VARCHAR(32) NOT NULL COMMENT 'Share ID (unique identifier used in the share link)',
    `document_id` BIGINT NOT NULL COMMENT 'Document ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'Share title',
    `share_type` TINYINT DEFAULT 1 COMMENT 'Share type (1-public link, 2-direct message share)',
    `share_code` VARCHAR(32) DEFAULT NULL COMMENT 'Share code (optional, used to increase security)',
    `expire_type` TINYINT DEFAULT 1 COMMENT 'Expiration type (1-permanent, 2-time-limited)',
    `expire_time` DATETIME DEFAULT NULL COMMENT 'Expiration time',
    `access_limit` INT DEFAULT 0 COMMENT 'Access count limit (0-unlimited)',
    `access_count` INT DEFAULT 0 COMMENT 'Number of accesses so far',
    `require_password` TINYINT DEFAULT 0 COMMENT 'Whether a password is required (0-no, 1-yes)',
    `password` VARCHAR(64) DEFAULT NULL COMMENT 'Access password (MD5 encrypted)',
    `sharer_id` BIGINT DEFAULT NULL COMMENT 'Sharer ID',
    `sharer_name` VARCHAR(64) DEFAULT NULL COMMENT 'Sharer name',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'Share description',
    `status` TINYINT DEFAULT 0 COMMENT 'Status (0-active, 1-expired, 2-deleted)',
    `share_time` DATETIME DEFAULT NULL COMMENT 'Share time',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    `create_by` BIGINT DEFAULT NULL COMMENT 'Creator ID',
    `update_by` BIGINT DEFAULT NULL COMMENT 'Updater ID',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Logical deletion flag (0-not deleted, 1-deleted)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_share_id` (`share_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_sharer_id` (`sharer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_share_time` (`share_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document share table';
