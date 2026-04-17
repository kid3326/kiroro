-- V3: 판매 거래 테이블 생성 (월별 파티셔닝 적용)
-- Requirements: 14.1 (시간별 Raw 데이터 저장), 14.3 (5년 데이터 보관)

-- 판매 거래 테이블 (파티셔닝 부모 테이블)
CREATE TABLE sales_transactions (
    id BIGSERIAL,
    sku VARCHAR(50) NOT NULL,
    transaction_time TIMESTAMP NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    return_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    channel VARCHAR(50),
    is_bundle BOOLEAN NOT NULL DEFAULT FALSE,
    bundle_id INTEGER,
    PRIMARY KEY (id, transaction_time)
) PARTITION BY RANGE (transaction_time);

-- 2024년 월별 파티션 생성
CREATE TABLE sales_transactions_2024_01 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE sales_transactions_2024_02 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE sales_transactions_2024_03 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE sales_transactions_2024_04 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE sales_transactions_2024_05 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE sales_transactions_2024_06 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE sales_transactions_2024_07 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE sales_transactions_2024_08 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE sales_transactions_2024_09 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE sales_transactions_2024_10 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE sales_transactions_2024_11 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE sales_transactions_2024_12 PARTITION OF sales_transactions
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025년 월별 파티션 생성
CREATE TABLE sales_transactions_2025_01 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE sales_transactions_2025_02 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE sales_transactions_2025_03 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE sales_transactions_2025_04 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE sales_transactions_2025_05 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE sales_transactions_2025_06 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE sales_transactions_2025_07 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE sales_transactions_2025_08 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE sales_transactions_2025_09 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE sales_transactions_2025_10 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE sales_transactions_2025_11 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE sales_transactions_2025_12 PARTITION OF sales_transactions
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 인덱스 (파티션 테이블에 자동 적용)
CREATE INDEX idx_sales_sku_time ON sales_transactions(sku, transaction_time);
CREATE INDEX idx_sales_channel_time ON sales_transactions(channel, transaction_time);
