ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS username VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS role_name VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS request_method VARCHAR(16) NULL,
    ADD COLUMN IF NOT EXISTS request_path VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS mac_address VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS host_name VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS location VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512) NULL,
    ADD COLUMN IF NOT EXISTS platform_type VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS operation_result VARCHAR(16) NULL;

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_username ON audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_type ON audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_ip_address ON audit_log(ip_address);
