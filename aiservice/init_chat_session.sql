CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    session_title VARCHAR(500),
    messages TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_important BOOLEAN DEFAULT FALSE,
    last_access_time DATETIME NOT NULL,
    INDEX idx_session_id (session_id),
    INDEX idx_last_access_time (last_access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;