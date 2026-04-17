-- V4: 광고비 및 재고 테이블 생성
-- Requirements: 14.1 (데이터 저장)

-- 광고 채널 ENUM 타입
CREATE TYPE ad_channel AS ENUM ('NAVER', 'GOOGLE', 'META', 'OTHERS');

-- 재고 평가 방법 ENUM 타입
CREATE TYPE valuation_method AS ENUM ('FIFO', 'LIFO', 'WEIGHTED_AVG');

-- 광고비 테이블
CREATE TABLE advertising_costs (
    id BIGSERIAL PRIMARY KEY,
    channel ad_channel NOT NULL,
    cost_date DATE NOT NULL,
    spend_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    impressions INTEGER NOT NULL DEFAULT 0,
    clicks INTEGER NOT NULL DEFAULT 0,
    conversions INTEGER NOT NULL DEFAULT 0,
    new_customers INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_ad_costs_channel_date ON advertising_costs(channel, cost_date);

-- 재고 테이블
CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    current_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER NOT NULL DEFAULT 0,
    unit_cost DECIMAL(15, 2) NOT NULL DEFAULT 0,
    valuation_method valuation_method NOT NULL DEFAULT 'WEIGHTED_AVG',
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_sku ON inventory(sku);
