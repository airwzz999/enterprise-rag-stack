-- =====================================================
-- kb_statistics database - seed sample data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_statistics`;

-- Seed document statistics data (based on already-created documents)
INSERT INTO `kb_document_statistics` (`id`, `document_id`, `document_title`, `view_count`, `like_count`, `comment_count`, `collect_count`, `share_count`, `stat_date`) VALUES
(1800000000000000001, 1000000000000000001, 'Spring Boot 3.x Quick Start Guide', 1523, 89, 23, 45, 12, CURDATE()),
(1800000000000000002, 1000000000000000002, 'React 18 + TypeScript Best Practices', 2187, 156, 45, 67, 23, CURDATE()),
(1800000000000000003, 1000000000000000003, 'MySQL 8.0 Performance Optimization Guide', 3421, 234, 67, 89, 34, CURDATE()),
(1800000000000000004, 1000000000000000004, 'Docker + Kubernetes Containerized Deployment', 1876, 98, 19, 34, 8, CURDATE()),
(1800000000000000005, 1000000000000000005, 'Enterprise Knowledge Base Product Requirements Document (PRD)', 987, 45, 12, 23, 5, CURDATE()),
(1800000000000000006, 1000000000000000006, 'UI Design Guidelines V2.0', 654, 34, 8, 12, 3, CURDATE()),
(1800000000000000007, 1000000000000000007, 'Document Review Process Guidelines', 1234, 67, 15, 34, 7, CURDATE()),
(1800000000000000008, 1000000000000000008, 'Employee Onboarding Guide', 5678, 234, 56, 89, 45, CURDATE()),
(1800000000000000009, 1000000000000000009, 'Expense Reimbursement Process Guide', 3456, 123, 34, 56, 21, CURDATE());

-- Seed user statistics data
INSERT INTO `kb_user_statistics` (`id`, `user_id`, `user_name`, `document_count`, `comment_count`, `like_count`, `view_count`, `login_count`, `stat_date`) VALUES
(1900000000000000001, 1000000000000000001, 'admin', 3, 15, 45, 2345, 67, CURDATE()),
(1900000000000000002, 1000000000000000004, 'developer', 2, 23, 89, 4523, 89, CURDATE()),
(1900000000000000003, 1000000000000000002, 'editor', 1, 12, 34, 1234, 45, CURDATE()),
(1900000000000000004, 1000000000000000006, 'designer', 1, 8, 34, 876, 23, CURDATE()),
(1900000000000000005, 1000000000000000005, 'product', 1, 6, 23, 1567, 34, CURDATE()),
(1900000000000000006, 1000000000000000003, 'tester', 0, 8, 15, 987, 12, CURDATE());

SELECT 'kb_statistics seed data initialization complete!' AS message;
SELECT CONCAT('Document statistics count: ', COUNT(*)) AS info FROM `kb_document_statistics`;
SELECT CONCAT('User statistics count: ', COUNT(*)) AS info FROM `kb_user_statistics`;
