-- V9: 내보내기 작업 테이블 생성
-- Requirements: 10.7 (내보내기 파일 관리)

-- 내보내기 형식 ENUM
CREATE TYPE export_format AS ENUM ('EXCEL', 'PDF', 'PPT');

-- 내보내기 상태 ENUM
CREATE TYPE export_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');

-- 내보내기 작업 테이블
CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    format export_format NOT NULL,
    status export_status NOT NULL DEFAULT 'PENDING',
    s3_key VARCHAR(500),
    download_url VARCHAR(2000),
    file_size_bytes BIGINT,
    filter_criteria TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    error_message VARCHAR(1000)
);

CREATE INDEX idx_export_jobs_user_id ON export_jobs(user_id);
CREATE INDEX idx_export_jobs_created_at ON export_jobs(created_at);
