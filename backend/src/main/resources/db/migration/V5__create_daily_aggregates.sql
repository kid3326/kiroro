-- V5: 일별 집계 테이블 생성 (월별 파티셔닝 적용)
-- Requirements: 14.2 (사전 집계 데이터 저장)

-- 일별 집계 테이블 (파티셔닝 부모 테이블)
CREATE TABLE daily_aggregates (
    id BIGSERIAL,
    aggregate_date DATE NOT NULL,
    sku VARCHAR(50),
    category VARCHAR(200),
    brand VARCHAR(200),
    total_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_revenue DECIMAL(15, 2) NOT NULL DEFAULT 0,
    cogs DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_profit DECIMAL(15, 2) NOT NULL DEFAULT 0,
    sales_volume INTEGER NOT NULL DEFAULT 0,
    ad_spend DECIMAL(15, 2) NOT NULL DEFAULT 0,
    roas DECIMAL(10, 4) NOT NULL DEFAULT 0,
    calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, aggregate_date)
) PARTITION BY RANGE (aggregate_date);

-- 2024년 월별 파티션 생성
CREATE TABLE daily_aggregates_2024_01 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE daily_aggregates_2024_02 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE daily_aggregates_2024_03 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE daily_aggregates_2024_04 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE daily_aggregates_2024_05 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE daily_aggregates_2024_06 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE daily_aggregates_2024_07 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE daily_aggregates_2024_08 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE daily_aggregates_2024_09 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE daily_aggregates_2024_10 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE daily_aggregates_2024_11 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE daily_aggregates_2024_12 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025년 월별 파티션 생성
CREATE TABLE daily_aggregates_2025_01 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE daily_aggregates_2025_02 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE daily_aggregates_2025_03 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE daily_aggregates_2025_04 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE daily_aggregates_2025_05 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE daily_aggregates_2025_06 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE daily_aggregates_2025_07 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE daily_aggregates_2025_08 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE daily_aggregates_2025_09 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE daily_aggregates_2025_10 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE daily_aggregates_2025_11 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE daily_aggregates_2025_12 PARTITION OF daily_aggregates
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 인덱스
CREATE INDEX idx_daily_agg_date_sku ON daily_aggregates(aggregate_date, sku);
CREATE INDEX idx_daily_agg_date_brand ON daily_aggregates(aggregate_date, brand);
