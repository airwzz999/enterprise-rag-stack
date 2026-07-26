-- ================================================
-- New service database table creation script
-- ================================================

-- 1. AI service related tables
USE knowledge_base_ai;

-- Conversation table
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    title VARCHAR(200) NOT NULL COMMENT 'Conversation title',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    model VARCHAR(50) NOT NULL COMMENT 'Model name',
    system_prompt TEXT COMMENT 'System prompt',
    tokens_used INT DEFAULT 0 COMMENT 'Token usage',
    message_count INT DEFAULT 0 COMMENT 'Message count',
    status TINYINT DEFAULT 0 COMMENT 'Status (0-in progress, 1-ended)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical deletion flag',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI conversation table';

-- Message table
CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    conversation_id BIGINT NOT NULL COMMENT 'Conversation ID',
    role VARCHAR(20) NOT NULL COMMENT 'Role type (system/user/assistant)',
    content TEXT NOT NULL COMMENT 'Message content',
    tokens INT DEFAULT 0 COMMENT 'Token count',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical deletion flag',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_create_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI message table';

-- AI feedback table
CREATE TABLE IF NOT EXISTS ai_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    conversation_id BIGINT NOT NULL COMMENT 'Conversation ID',
    message_id BIGINT NOT NULL COMMENT 'Message ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    feedback_type VARCHAR(20) NOT NULL COMMENT 'Feedback type (positive/negative)',
    feedback_content TEXT COMMENT 'Feedback content',
    rating INT COMMENT 'Rating (1-5)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical deletion flag',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI feedback table';

-- 2. Statistics service related tables
USE knowledge_base_statistics;

-- System statistics table
CREATE TABLE IF NOT EXISTS system_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    stat_date DATE NOT NULL COMMENT 'Statistics date',
    document_count INT DEFAULT 0 COMMENT 'Total document count',
    user_count INT DEFAULT 0 COMMENT 'Total user count',
    visit_count INT DEFAULT 0 COMMENT 'Total visit count',
    active_user_count INT DEFAULT 0 COMMENT 'Active user count',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System statistics table';

-- Document statistics table
CREATE TABLE IF NOT EXISTS document_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    stat_date DATE NOT NULL COMMENT 'Statistics date',
    category_id BIGINT COMMENT 'Category ID',
    create_count INT DEFAULT 0 COMMENT 'Create count',
    update_count INT DEFAULT 0 COMMENT 'Update count',
    delete_count INT DEFAULT 0 COMMENT 'Delete count',
    view_count INT DEFAULT 0 COMMENT 'View count',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    INDEX idx_stat_date (stat_date),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document statistics table';

-- User statistics table
CREATE TABLE IF NOT EXISTS user_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    stat_date DATE NOT NULL COMMENT 'Statistics date',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    login_count INT DEFAULT 0 COMMENT 'Login count',
    document_create_count INT DEFAULT 0 COMMENT 'Document creation count',
    document_view_count INT DEFAULT 0 COMMENT 'Document view count',
    online_duration INT DEFAULT 0 COMMENT 'Online duration (seconds)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    INDEX idx_stat_date (stat_date),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User statistics table';

-- 3. Notification service related tables
USE knowledge_base_notification;

-- Notification table
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    title VARCHAR(200) NOT NULL COMMENT 'Notification title',
    content TEXT NOT NULL COMMENT 'Notification content',
    type VARCHAR(50) NOT NULL COMMENT 'Notification type (system/user/document/review)',
    receiver_id BIGINT NOT NULL COMMENT 'Receiver ID',
    sender_id BIGINT COMMENT 'Sender ID',
    is_read TINYINT DEFAULT 0 COMMENT 'Whether read (0-unread, 1-read)',
    read_time DATETIME COMMENT 'Read time',
    related_id BIGINT COMMENT 'Related ID',
    related_type VARCHAR(50) COMMENT 'Related type',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical deletion flag',
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification table';

-- Notification template table
CREATE TABLE IF NOT EXISTS notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    code VARCHAR(50) NOT NULL COMMENT 'Template code',
    name VARCHAR(100) NOT NULL COMMENT 'Template name',
    title VARCHAR(200) NOT NULL COMMENT 'Notification title template',
    content TEXT NOT NULL COMMENT 'Notification content template',
    type VARCHAR(50) NOT NULL COMMENT 'Notification type',
    status TINYINT DEFAULT 1 COMMENT 'Status (0-disabled, 1-enabled)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification template table';

-- 4. Common module related tables
USE knowledge_base_common;

-- Operation log table
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key ID',
    module VARCHAR(100) COMMENT 'Operation module',
    operation_type VARCHAR(50) COMMENT 'Operation type',
    description VARCHAR(500) COMMENT 'Operation description',
    method VARCHAR(200) COMMENT 'Request method',
    url VARCHAR(500) COMMENT 'Request URL',
    params TEXT COMMENT 'Request parameters',
    result TEXT COMMENT 'Response result',
    user_id BIGINT COMMENT 'Operating user ID',
    username VARCHAR(100) COMMENT 'Operating username',
    ip VARCHAR(50) COMMENT 'Operating user IP',
    location VARCHAR(200) COMMENT 'Operation location',
    browser VARCHAR(50) COMMENT 'Browser type',
    os VARCHAR(50) COMMENT 'Operating system',
    duration BIGINT COMMENT 'Execution duration (milliseconds)',
    status TINYINT COMMENT 'Operation status (0-failure, 1-success)',
    error_msg TEXT COMMENT 'Error message',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
    tenant_id BIGINT COMMENT 'Tenant ID',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (created_at),
    INDEX idx_module (module),
    INDEX idx_operation_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation log table';

-- Insert default notification templates
INSERT INTO notification_template (code, name, title, content, type) VALUES
('DOCUMENT_REVIEW', 'Document Review Notification', 'Document Review Reminder', 'Your document "{documentTitle}" has been submitted for review. Please wait patiently for the review result.', 'document'),
('DOCUMENT_APPROVED', 'Document Review Approved', 'Document Review Approved', 'Your document "{documentTitle}" has passed review.', 'document'),
('DOCUMENT_REJECTED', 'Document Review Rejected', 'Document Review Rejected', 'Your document "{documentTitle}" did not pass review. Reason: {reviewComment}', 'document'),
('SYSTEM_NOTICE', 'System Notification', '{title}', '{content}', 'system');
