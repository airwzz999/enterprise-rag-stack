-- Notification template table
-- Used to manage template configuration for system notification messages

CREATE TABLE IF NOT EXISTS kb_notification_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key ID',
    template_code VARCHAR(100) NOT NULL COMMENT 'Template code',
    template_name VARCHAR(200) NOT NULL COMMENT 'Template name',
    notification_type VARCHAR(50) NOT NULL COMMENT 'Notification type: EMAIL/SMS/WECHAT/SYSTEM/BROWSER',
    title VARCHAR(500) NOT NULL COMMENT 'Template title',
    content TEXT NOT NULL COMMENT 'Template content',
    variables VARCHAR(1000) DEFAULT '[]' COMMENT 'Template variables (JSON array format)',
    description VARCHAR(500) DEFAULT NULL COMMENT 'Template description',
    is_active TINYINT(1) DEFAULT 1 COMMENT 'Whether enabled: 0-disabled, 1-enabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification template table';

-- Seed default template data
INSERT INTO kb_notification_template (template_code, template_name, notification_type, title, content, variables, description, is_active) VALUES
('EMAIL_VERIFY_CODE', 'Email verification code', 'EMAIL', 'Verification Code - {{systemName}}', 'Dear {{userName}}, your verification code is: {{verifyCode}}, valid for 5 minutes.', '["userName","verifyCode","systemName"]', 'Used for email verification and password recovery scenarios', 1),
('DOCUMENT_APPROVED', 'Document review approved', 'SYSTEM', 'Your document "{{documentTitle}}" has passed review', 'Your submitted document "{{documentTitle}}" has passed review. Thank you for your contribution!', '["documentTitle"]', 'Notification sent when a document review is approved', 1),
('DOCUMENT_REJECTED', 'Document review rejected', 'SYSTEM', 'Your document "{{documentTitle}}" needs revision', 'Your submitted document "{{documentTitle}}" did not pass review. Reason: {{rejectReason}}. Please revise and resubmit.', '["documentTitle","rejectReason"]', 'Notification sent when a document review is rejected', 1),
('NEW_COMMENT', 'New comment notification', 'SYSTEM', 'Your document received a new comment', '{{commentUsername}} commented on your document "{{documentTitle}}": {{commentContent}}', '["commentUsername","documentTitle","commentContent"]', 'Notification sent when a document receives a new comment', 1),
('DOCUMENT_LIKED', 'Document liked', 'SYSTEM', 'Your document received a new like', '{{likeUsername}} liked your document "{{documentTitle}}"', '["likeUsername","documentTitle"]', 'Notification sent when a document is liked', 1),
('WELCOME_MESSAGE', 'Welcome message', 'SYSTEM', 'Welcome to {{systemName}}', 'Dear {{userName}}, welcome to {{systemName}}! We look forward to your contributions.', '["userName","systemName"]', 'Welcome message sent after a user registers', 1);
