-- V10: alerts 테이블의 user_id를 nullable로 변경
-- 시스템 전체 알림은 특정 사용자에 귀속되지 않을 수 있음

ALTER TABLE alerts ALTER COLUMN user_id DROP NOT NULL;
