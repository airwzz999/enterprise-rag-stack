-- =====================================================
-- kb_file database - file service
-- =====================================================

SET NAMES utf8mb4;
USE `kb_file`;
SET FOREIGN_KEY_CHECKS = 0;

-- File information table
DROP TABLE IF EXISTS `kb_file`;
CREATE TABLE `kb_file` (
  `id` BIGINT NOT NULL COMMENT 'File ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT 'File name',
  `original_name` VARCHAR(255) NOT NULL COMMENT 'Original file name',
  `file_path` VARCHAR(500) NOT NULL COMMENT 'File path',
  `file_size` BIGINT NOT NULL COMMENT 'File size (bytes)',
  `file_type` VARCHAR(50) NOT NULL COMMENT 'File type',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
  `file_extension` VARCHAR(20) DEFAULT NULL COMMENT 'File extension',
  `storage_type` VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT 'Storage type',
  `upload_user_id` BIGINT NOT NULL COMMENT 'Uploading user ID',
  `upload_user_name` VARCHAR(50) DEFAULT NULL COMMENT 'Uploading user name (redundant field)',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT 'Related type',
  `related_id` BIGINT DEFAULT NULL COMMENT 'Related ID',
  `download_count` INT NOT NULL DEFAULT 0 COMMENT 'Download count',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deletion flag',
  PRIMARY KEY (`id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_related` (`related_type`, `related_id`),
  KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File information table';

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kb_file database tables created!' AS message;
