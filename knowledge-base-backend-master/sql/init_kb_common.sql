-- =====================================================
-- kb_common database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_common`;

-- Seed system configuration data
INSERT INTO `kb_system_config` (`id`, `config_key`, `config_value`, `config_type`, `category`, `description`, `is_public`) VALUES
(1300000000000000001, 'site.name', 'Enterprise Knowledge Base', 'string', 'basic', 'Site name', 1),
(1300000000000000002, 'site.logo', '/logo.png', 'string', 'basic', 'Site logo', 1),
(1300000000000000003, 'site.allowRegister', 'true', 'boolean', 'basic', 'Allow user registration', 1),
(1300000000000000004, 'upload.maxSize', '104857600', 'number', 'upload', 'Maximum upload file size (bytes)', 0),
(1300000000000000005, 'upload.allowTypes', '.doc,.docx,.pdf,.txt,.md,.png,.jpg,.jpeg', 'string', 'upload', 'Allowed file types', 0),
(1300000000000000006, 'security.sessionTimeout', '7200', 'number', 'security', 'Session timeout (seconds)', 0),
(1300000000000000007, 'security.passwordMinLength', '8', 'number', 'security', 'Minimum password length', 0),
(1300000000000000008, 'email.enabled', 'false', 'boolean', 'email', 'Enable email notifications', 0),
(1300000000000000009, 'email.host', 'smtp.example.com', 'string', 'email', 'SMTP server', 0),
(1300000000000000010, 'email.port', '587', 'number', 'email', 'SMTP port', 0),
(1300000000000000011, 'ai.model', 'qwen-turbo', 'string', 'ai', 'AI model name', 0),
(1300000000000000012, 'ai.maxTokens', '2000', 'number', 'ai', 'Max AI token count', 0);

-- Seed dictionary data
-- Document status dictionary
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`) VALUES
(1400000000000000001, 'document_status', 'Document status', 'document', 'Document status enum', 1),
(1400000000000000002, 'review_status', 'Review status', 'review', 'Review status enum', 2),
(1400000000000000003, 'notification_type', 'Notification type', 'notification', 'Notification type enum', 3);

INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `status`) VALUES
-- Document status
(1400000000000000001, 1400000000000000001, 'Draft', 'draft', 1, 'default', 1),
(1400000000000000002, 1400000000000000001, 'Published', 'published', 2, 'success', 1),
(1400000000000000003, 1400000000000000001, 'Archived', 'archived', 3, 'info', 1),
-- Review status
(1400000000000000004, 1400000000000000002, 'Pending review', 'pending', 1, 'warning', 1),
(1400000000000000005, 1400000000000000002, 'Approved', 'approved', 2, 'success', 1),
(1400000000000000006, 1400000000000000002, 'Rejected', 'rejected', 3, 'error', 1),
-- Notification type
(1400000000000000007, 1400000000000000003, 'System notification', 'system', 1, 'blue', 1),
(1400000000000000008, 1400000000000000003, 'Comment notification', 'comment', 2, 'green', 1),
(1400000000000000009, 1400000000000000003, 'Mention notification', 'mention', 3, 'orange', 1),
(1400000000000000010, 1400000000000000003, 'Review notification', 'review', 4, 'purple', 1),
(1400000000000000011, 1400000000000000003, 'Like notification', 'like', 5, 'red', 1);

SELECT 'kb_common seed data initialization complete!' AS message;
SELECT CONCAT('Config item count: ', COUNT(*)) AS info FROM `kb_system_config`;
SELECT CONCAT('Dictionary type count: ', COUNT(*)) AS info FROM `kb_dict`;
