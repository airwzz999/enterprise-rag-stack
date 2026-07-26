-- =====================================================
-- kb_foundation database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_foundation`;

-- =====================================================
-- 1. Seed system configuration data
-- =====================================================
INSERT INTO `kb_system_config` (`id`, `config_key`, `config_value`, `config_type`, `category`, `description`, `is_public`) VALUES
-- AI configuration
(2000000000000000001, 'qwen.api.key', '', 'string', 'AI', 'Qwen API key', 0),
(2000000000000000002, 'qwen.model.name', 'qwen-max', 'string', 'AI', 'Qwen model name', 1),
(2000000000000000003, 'qwen.embedding.model', 'text-embedding-v3', 'string', 'AI', 'Qwen embedding model', 1),
(2000000000000000004, 'milvus.host', 'localhost', 'string', 'AI', 'Milvus host address', 1),
(2000000000000000005, 'milvus.port', '19530', 'number', 'AI', 'Milvus port', 1),

-- Storage configuration
(2000000000000000006, 'rustfs.endpoints', 'http://localhost:8200', 'json', 'STORAGE', 'RustFS endpoint list', 1),
(2000000000000000007, 'rustfs.bucket', 'knowledge-docs', 'string', 'STORAGE', 'RustFS storage bucket', 1),
(2000000000000000008, 'file.upload.max.size', '52428800', 'number', 'STORAGE', 'Maximum file upload size (bytes)', 1),
(2000000000000000009, 'file.upload.allowed.types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md', 'string', 'STORAGE', 'Allowed file upload types', 1),

-- Notification configuration
(2000000000000000010, 'email.enabled', 'true', 'boolean', 'NOTIFICATION', 'Whether email notifications are enabled', 1),
(2000000000000000011, 'email.host', 'smtp.example.com', 'string', 'NOTIFICATION', 'Mail server address', 0),
(2000000000000000012, 'email.port', '587', 'number', 'NOTIFICATION', 'Mail server port', 0),
(2000000000000000013, 'notification.retention.days', '90', 'number', 'NOTIFICATION', 'Notification retention days', 1),
(2000000000000000014, 'websocket.enabled', 'true', 'boolean', 'NOTIFICATION', 'Whether WebSocket push is enabled', 1),

-- Security configuration
(2000000000000000015, 'auth.session.timeout', '7200', 'number', 'SECURITY', 'Session timeout (seconds)', 1),
(2000000000000000016, 'auth.password.min.length', '8', 'number', 'SECURITY', 'Minimum password length', 1),
(2000000000000000017, 'auth.password.require.special', 'true', 'boolean', 'SECURITY', 'Whether the password requires special characters', 1),
(2000000000000000018, 'auth.login.max.retry', '5', 'number', 'SECURITY', 'Maximum login retry attempts', 1),

-- System configuration
(2000000000000000019, 'system.name', 'Enterprise Knowledge Base', 'string', 'SYSTEM', 'System name', 1),
(2000000000000000020, 'system.version', '1.0.0', 'string', 'SYSTEM', 'System version', 1),
(2000000000000000021, 'system.logo', '/logo.png', 'string', 'SYSTEM', 'System logo path', 1),
(2000000000000000022, 'user.registration.enabled', 'true', 'boolean', 'SYSTEM', 'Whether user registration is allowed', 1),
(2000000000000000023, 'user.default.role', 'VIEWER', 'string', 'SYSTEM', 'Default role for new users', 1);

-- =====================================================
-- 2. Seed dictionary type data
-- =====================================================
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`, `status`) VALUES
(3000000000000000001, 'document_status', 'Document status', 'DOCUMENT', 'Document status: draft/pending review/published/rejected', 1, 1),
(3000000000000000002, 'notification_type', 'Notification type', 'SYSTEM', 'System notification type', 2, 1),
(3000000000000000003, 'operation_type', 'Operation type', 'SYSTEM', 'System operation type', 3, 1),
(3000000000000000004, 'file_type', 'File type', 'FILE', 'Supported file types', 4, 1),
(3000000000000000005, 'user_type', 'User type', 'USER', 'User type classification', 5, 1);

-- =====================================================
-- 3. Seed dictionary data
-- =====================================================

-- Document status dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3100000000000000001, 3000000000000000001, 'document_status', 'Draft', '0', 1, 'badge-gray', 1, 1),
(3100000000000000002, 3000000000000000001, 'document_status', 'Pending review', '1', 2, 'badge-yellow', 0, 1),
(3100000000000000003, 3000000000000000001, 'document_status', 'Published', '2', 3, 'badge-green', 0, 1),
(3100000000000000004, 3000000000000000001, 'document_status', 'Rejected', '3', 4, 'badge-red', 0, 1);

-- Notification type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3200000000000000001, 3000000000000000002, 'notification_type', 'System notification', 'system', 1, 'badge-blue', 1, 1),
(3200000000000000002, 3000000000000000002, 'notification_type', 'Comment notification', 'comment', 2, 'badge-green', 0, 1),
(3200000000000000003, 3000000000000000002, 'notification_type', '@Mention', 'mention', 3, 'badge-orange', 0, 1),
(3200000000000000004, 3000000000000000002, 'notification_type', 'Review notification', 'review', 4, 'badge-purple', 0, 1),
(3200000000000000005, 3000000000000000002, 'notification_type', 'Like notification', 'like', 5, 'badge-pink', 0, 1);

-- Operation type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3300000000000000001, 3000000000000000003, 'operation_type', 'Login', 'LOGIN', 1, NULL, 0, 1),
(3300000000000000002, 3000000000000000003, 'operation_type', 'Logout', 'LOGOUT', 2, NULL, 0, 1),
(3300000000000000003, 3000000000000000003, 'operation_type', 'Create', 'CREATE', 3, NULL, 0, 1),
(3300000000000000004, 3000000000000000003, 'operation_type', 'Update', 'UPDATE', 4, NULL, 0, 1),
(3300000000000000005, 3000000000000000003, 'operation_type', 'Delete', 'DELETE', 5, NULL, 0, 1),
(3300000000000000006, 3000000000000000003, 'operation_type', 'Query', 'QUERY', 6, NULL, 0, 1),
(3300000000000000007, 3000000000000000003, 'operation_type', 'Export', 'EXPORT', 7, NULL, 0, 1),
(3300000000000000008, 3000000000000000003, 'operation_type', 'Import', 'IMPORT', 8, NULL, 0, 1);

-- File type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3400000000000000001, 3000000000000000004, 'file_type', 'PDF Document', 'pdf', 1, 'file-pdf', 1, 1),
(3400000000000000002, 3000000000000000004, 'file_type', 'Word Document', 'doc', 2, 'file-word', 0, 1),
(3400000000000000003, 3000000000000000004, 'file_type', 'Excel Spreadsheet', 'xls', 3, 'file-excel', 0, 1),
(3400000000000000004, 3000000000000000004, 'file_type', 'PPT Presentation', 'ppt', 4, 'file-ppt', 0, 1),
(3400000000000000005, 3000000000000000004, 'file_type', 'Image', 'image', 5, 'file-image', 0, 1),
(3400000000000000006, 3000000000000000004, 'file_type', 'Video', 'video', 6, 'file-video', 0, 1),
(3400000000000000007, 3000000000000000004, 'file_type', 'Text', 'txt', 7, 'file-text', 0, 1),
(3400000000000000008, 3000000000000000004, 'file_type', 'Markdown', 'md', 8, 'file-markdown', 0, 1);

-- User type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3500000000000000001, 3000000000000000005, 'user_type', 'Super Admin', 'SUPER_ADMIN', 1, 'user-admin', 0, 1),
(3500000000000000002, 3000000000000000005, 'user_type', 'Knowledge Admin', 'KNOWLEDGE_ADMIN', 2, 'user-manager', 0, 1),
(3500000000000000003, 3000000000000000005, 'user_type', 'Content Admin', 'CONTENT_ADMIN', 3, 'user-editor', 0, 1),
(3500000000000000004, 3000000000000000005, 'user_type', 'Team Leader', 'TEAM_LEADER', 4, 'user-leader', 0, 1),
(3500000000000000005, 3000000000000000005, 'user_type', 'Contributor', 'CONTRIBUTOR', 5, 'user-contributor', 0, 1),
(3500000000000000006, 3000000000000000005, 'user_type', 'Regular User', 'VIEWER', 6, 'user-viewer', 1, 1);

-- =====================================================
-- 4. Seed notification data
-- =====================================================
INSERT INTO `kb_notification` (`id`, `user_id`, `user_name`, `notification_type`, `title`, `content`, `link`, `is_read`) VALUES
(1500000000000000001, 1000000000000000002, 'editor', 'system', 'Welcome to the Enterprise Knowledge Base', 'Welcome to the Enterprise Knowledge Base system, start your knowledge management journey!', '/documents', 0),
(1500000000000000002, 1000000000000000004, 'developer', 'comment', 'Your document received a new comment', '"Spring Boot 3.x Quick Start Guide" received a new comment', '/documents/1000000000000000001', 0),
(1500000000000000003, 1000000000000000005, 'product', 'review', 'Document review approved', 'Your "Enterprise Knowledge Base Product Requirements Document (PRD)" has passed review', '/documents/1000000000000000005', 1),
(1500000000000000004, 1000000000000000001, 'admin', 'mention', 'Someone mentioned you', 'developer mentioned you in "Docker + Kubernetes Containerized Deployment"', '/documents/1000000000000000004', 0);

-- =====================================================
-- 5. Seed operation log data
-- =====================================================
INSERT INTO `kb_operation_log` (`id`, `module`, `operation_type`, `operation_desc`, `request_method`, `request_url`, `user_id`, `username`, `ip_address`, `execute_time`, `status`) VALUES
(4000000000000000001, 'User Management', 'LOGIN', 'User login', 'POST', '/api/auth/login', 1000000000000000001, 'admin', '127.0.0.1', 125, 1),
(4000000000000000002, 'Document Management', 'CREATE', 'Create document', 'POST', '/api/document', 1000000000000000002, 'editor', '127.0.0.1', 342, 1),
(4000000000000000003, 'Document Management', 'UPDATE', 'Update document', 'PUT', '/api/document/1000000000000000001', 1000000000000000002, 'editor', '127.0.0.1', 215, 1),
(4000000000000000004, 'System Configuration', 'UPDATE', 'Update system configuration', 'PUT', '/api/foundation/config', 1000000000000000001, 'admin', '127.0.0.1', 89, 1),
(4000000000000000005, 'User Management', 'CREATE', 'Create user', 'POST', '/api/auth/user', 1000000000000000001, 'admin', '127.0.0.1', 156, 1);

SELECT 'kb_foundation seed data initialization complete!' AS message;
SELECT CONCAT('System config count: ', COUNT(*)) AS info FROM `kb_system_config`;
SELECT CONCAT('Dictionary type count: ', COUNT(*)) AS info FROM `kb_dict`;
SELECT CONCAT('Dictionary data count: ', COUNT(*)) AS info FROM `kb_dict_data`;
SELECT CONCAT('Notification count: ', COUNT(*)) AS info FROM `kb_notification`;
SELECT CONCAT('Operation log count: ', COUNT(*)) AS info FROM `kb_operation_log`;
