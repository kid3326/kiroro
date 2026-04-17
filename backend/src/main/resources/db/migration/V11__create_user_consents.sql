-- 개인정보 수집 동의 테이블
-- 개인정보보호법 준수를 위한 동의 기록 저장
-- Requirements: 15.5

CREATE TABLE IF NOT EXISTS user_consents (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    consent_type VARCHAR(50) NOT NULL,
    consented BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    consented_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, consent_type)
);

CREATE INDEX idx_user_consents_user_id ON user_consents(user_id);
CREATE INDEX idx_user_consents_type ON user_consents(consent_type);
