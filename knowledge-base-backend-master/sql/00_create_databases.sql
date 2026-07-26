-- =====================================================
-- Enterprise Knowledge Base System - create all microservice databases
-- =====================================================

SET NAMES utf8mb4;

-- Drop existing databases (use with caution!)
-- DROP DATABASE IF EXISTS kb_user;
-- DROP DATABASE IF EXISTS kb_document;
-- DROP DATABASE IF EXISTS kb_search;
-- DROP DATABASE IF EXISTS kb_file;
-- DROP DATABASE IF EXISTS kb_ai;
-- DROP DATABASE IF EXISTS kb_statistics;
-- DROP DATABASE IF EXISTS kb_notification;
-- DROP DATABASE IF EXISTS kb_graph;
-- DROP DATABASE IF EXISTS kb_common;

-- Create all microservice databases
CREATE DATABASE IF NOT EXISTS `kb_user`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_document`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_search`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_file`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_ai`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_statistics`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_notification`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_graph`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `kb_common`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Foundation service database (merges kb_common and kb_notification)
CREATE DATABASE IF NOT EXISTS `kb_foundation`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Create dedicated database users (optional, for production use)

-- User auth service user
CREATE USER IF NOT EXISTS 'kb_user_service'@'%' IDENTIFIED BY 'kb_user_2024';
GRANT ALL PRIVILEGES ON kb_user.* TO 'kb_user_service'@'%';

-- Document management service user
CREATE USER IF NOT EXISTS 'kb_document_service'@'%' IDENTIFIED BY 'kb_document_2024';
GRANT ALL PRIVILEGES ON kb_document.* TO 'kb_document_service'@'%';

-- Search service user
CREATE USER IF NOT EXISTS 'kb_search_service'@'%' IDENTIFIED BY 'kb_search_2024';
GRANT ALL PRIVILEGES ON kb_search.* TO 'kb_search_service'@'%';

-- File service user
CREATE USER IF NOT EXISTS 'kb_file_service'@'%' IDENTIFIED BY 'kb_file_2024';
GRANT ALL PRIVILEGES ON kb_file.* TO 'kb_file_service'@'%';

-- AI service user
CREATE USER IF NOT EXISTS 'kb_ai_service'@'%' IDENTIFIED BY 'kb_ai_2024';
GRANT ALL PRIVILEGES ON kb_ai.* TO 'kb_ai_service'@'%';

-- Statistics service user
CREATE USER IF NOT EXISTS 'kb_statistics_service'@'%' IDENTIFIED BY 'kb_statistics_2024';
GRANT ALL PRIVILEGES ON kb_statistics.* TO 'kb_statistics_service'@'%';

-- Notification service user
CREATE USER IF NOT EXISTS 'kb_notification_service'@'%' IDENTIFIED BY 'kb_notification_2024';
GRANT ALL PRIVILEGES ON kb_notification.* TO 'kb_notification_service'@'%';

-- Graph service user
CREATE USER IF NOT EXISTS 'kb_graph_service'@'%' IDENTIFIED BY 'kb_graph_2024';
GRANT ALL PRIVILEGES ON kb_graph.* TO 'kb_graph_service'@'%';

-- Common module user (read-only access for all services)
CREATE USER IF NOT EXISTS 'kb_common_reader'@'%' IDENTIFIED BY 'kb_common_2024';
GRANT SELECT ON kb_common.* TO 'kb_common_reader'@'%';

-- Foundation service user
CREATE USER IF NOT EXISTS 'kb_foundation_service'@'%' IDENTIFIED BY 'kb_foundation_2024';
GRANT ALL PRIVILEGES ON kb_foundation.* TO 'kb_foundation_service'@'%';

FLUSH PRIVILEGES;

-- Show the creation results
SELECT 'Database creation complete!' AS message;
SHOW DATABASES LIKE 'kb_%';

SELECT 'Database user creation complete!' AS message;
SELECT user, host FROM mysql.user WHERE user LIKE 'kb_%';
