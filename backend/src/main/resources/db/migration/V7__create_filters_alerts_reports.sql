-- V7: 필터, 알림, 리포트 테이블 생성
-- Requirements: 14.1 (데이터 저장)

-- 알림 타입 ENUM
CREATE TYPE alert_type AS ENUM ('REVENUE', 'AD_SPEND', 'ANOMALY', 'INVENTORY');

-- 알림 심각도 ENUM
CREATE TYPE alert_severity AS ENUM ('CRITICAL', 'IMPORTANT', 'INFORMATIONAL');

-- 리포트 빈도 ENUM
CREATE TYPE report_frequency AS ENUM ('DAILY', 'WEEKLY');

-- 리포트 상태 ENUM
CREATE TYPE report_status AS ENUM ('GENERATED', 'SENT', 'FAILED');

-- 저장된 필터 테이블
CREATE TABLE saved_filters (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    filter_criteria JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saved_filters_user_id ON saved_filters(user_id);

-- 알림 설정 테이블
CREATE TABLE alert_configs (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    alert_type alert_type NOT NULL,
    threshold_value DECIMAL(15, 2) NOT NULL,
    severity alert_severity NOT NULL DEFAULT 'INFORMATIONAL',
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_alert_configs_user_id ON alert_configs(user_id);

-- 알림 테이블
CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    config_id INTEGER REFERENCES alert_configs(id) ON DELETE SET NULL,
    severity alert_severity NOT NULL,
    title VARCHAR(300) NOT NULL,
    message TEXT,
    is_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMP
);

CREATE INDEX idx_alerts_user_id ON alerts(user_id);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at);

-- 리포트 스케줄 테이블
CREATE TABLE report_schedules (
    id SERIAL PRIMARY KEY,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_name VARCHAR(200) NOT NULL,
    frequency report_frequency NOT NULL,
    scheduled_time TIME NOT NULL,
    recipients JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_schedules_created_by ON report_schedules(created_by);

-- 리포트 이력 테이블
CREATE TABLE report_history (
    id BIGSERIAL PRIMARY KEY,
    schedule_id INTEGER REFERENCES report_schedules(id) ON DELETE SET NULL,
    file_url VARCHAR(1000),
    file_size_bytes BIGINT,
    status report_status NOT NULL DEFAULT 'GENERATED',
    generated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP
);

CREATE INDEX idx_report_history_schedule_id ON report_history(schedule_id);
CREATE INDEX idx_report_history_generated_at ON report_history(generated_at);
