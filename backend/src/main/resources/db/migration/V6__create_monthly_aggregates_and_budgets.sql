-- V6: 월별 집계 및 예산 목표 테이블 생성
-- Requirements: 14.2 (사전 집계 데이터 저장)

-- 예산 메트릭 타입 ENUM
CREATE TYPE budget_metric_type AS ENUM ('REVENUE', 'AD_SPEND', 'PROFIT');

-- 월별 집계 테이블
CREATE TABLE monthly_aggregates (
    id BIGSERIAL PRIMARY KEY,
    aggregate_month DATE NOT NULL,
    category VARCHAR(200),
    brand VARCHAR(200),
    total_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    cogs DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    ebitda DECIMAL(15, 2) NOT NULL DEFAULT 0,
    operating_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_ad_spend DECIMAL(15, 2) NOT NULL DEFAULT 0,
    avg_roas DECIMAL(10, 4) NOT NULL DEFAULT 0,
    cac DECIMAL(15, 2) NOT NULL DEFAULT 0,
    budget_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    calculated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monthly_agg_month ON monthly_aggregates(aggregate_month);

-- 예산 목표 테이블
CREATE TABLE budget_targets (
    id SERIAL PRIMARY KEY,
    target_month DATE NOT NULL,
    category VARCHAR(200),
    metric_type budget_metric_type NOT NULL,
    target_value DECIMAL(15, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_budget_targets_month ON budget_targets(target_month);
