-- =====================================================
-- kb_notification database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_notification`;

-- Seed notification data
INSERT INTO `kb_notification` (`id`, `user_id`, `user_name`, `notification_type`, `title`, `content`, `link`, `is_read`) VALUES
(1500000000000000001, 1000000000000000002, 'editor', 'system', 'Welcome to the Enterprise Knowledge Base', 'Welcome to the Enterprise Knowledge Base system, start your knowledge management journey!', '/documents', 0),
(1500000000000000002, 1000000000000000004, 'developer', 'comment', 'Your document received a new comment', '"Spring Boot 3.x Quick Start Guide" received a new comment', '/documents/1000000000000000001', 0),
(1500000000000000003, 1000000000000000005, 'product', 'review', 'Document review approved', 'Your "Enterprise Knowledge Base Product Requirements Document (PRD)" has passed review', '/documents/1000000000000000005', 1),
(1500000000000000004, 1000000000000000001, 'admin', 'mention', 'Someone mentioned you', 'developer mentioned you in "Docker + Kubernetes Containerized Deployment"', '/documents/1000000000000000004', 0);

SELECT 'kb_notification seed data initialization complete!' AS message;
SELECT CONCAT('Notification count: ', COUNT(*)) AS info FROM `kb_notification`;
