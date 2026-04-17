-- V12: 대시보드 시드 데이터 삽입
-- 사용자, 상품 계층, 판매 거래, 광고비, 재고, 일별/월별 집계, 예산 목표 데이터

-- ============================================================
-- 1. 사용자 데이터 (역할별 1명씩, 비밀번호: Password1)
--    bcrypt hash for 'Password1' with cost factor 12
-- ============================================================
INSERT INTO users (id, username, password_hash, email, role, assigned_brand, is_active) VALUES
('a0000000-0000-0000-0000-000000000001', 'ceo_kim', '$2a$12$LJ3m4ys3Gzv0hRYGnKVKnOQZHXfGqU8bKJl5d1sBzKqFz5HhWvthW', 'ceo@retaildashboard.com', 'CEO', NULL, TRUE),
('a0000000-0000-0000-0000-000000000002', 'exec_park', '$2a$12$LJ3m4ys3Gzv0hRYGnKVKnOQZHXfGqU8bKJl5d1sBzKqFz5HhWvthW', 'exec@retaildashboard.com', 'EXECUTIVE', NULL, TRUE),
('a0000000-0000-0000-0000-000000000003', 'mkt_lee', '$2a$12$LJ3m4ys3Gzv0hRYGnKVKnOQZHXfGqU8bKJl5d1sBzKqFz5HhWvthW', 'marketing@retaildashboard.com', 'MARKETING', '무신사 스탠다드', TRUE),
('a0000000-0000-0000-0000-000000000004', 'fin_choi', '$2a$12$LJ3m4ys3Gzv0hRYGnKVKnOQZHXfGqU8bKJl5d1sBzKqFz5HhWvthW', 'finance@retaildashboard.com', 'FINANCE', NULL, TRUE),
('a0000000-0000-0000-0000-000000000005', 'prod_jung', '$2a$12$LJ3m4ys3Gzv0hRYGnKVKnOQZHXfGqU8bKJl5d1sBzKqFz5HhWvthW', 'product@retaildashboard.com', 'PRODUCT', '커버낫', TRUE);

-- ============================================================
-- 2. 상품 계층 데이터
-- ============================================================
-- 카테고리
INSERT INTO product_categories (id, name, description) VALUES
(1, '의류', '패션 의류 전체 카테고리');

-- 서브카테고리
INSERT INTO product_subcategories (id, category_id, name) VALUES
(1, 1, '상의'),
(2, 1, '하의'),
(3, 1, '아우터');

-- 브랜드
INSERT INTO brands (id, subcategory_id, name) VALUES
(1, 1, '무신사 스탠다드'),
(2, 1, '커버낫'),
(3, 1, '디스이즈네버댓'),
(4, 2, '무신사 스탠다드'),
(5, 2, '스파오'),
(6, 2, '유니클로'),
(7, 3, '내셔널지오그래픽'),
(8, 3, '노스페이스'),
(9, 3, '디스커버리');

-- 상품 (SKU)
INSERT INTO products (id, brand_id, sku, name, variant_color, variant_size, unit_cost, is_active) VALUES
(1,  1, 'MS-TOP-001', '에센셜 반팔 티셔츠', '화이트', 'M', 15000, TRUE),
(2,  1, 'MS-TOP-002', '에센셜 반팔 티셔츠', '블랙',   'L', 15000, TRUE),
(3,  1, 'MS-TOP-003', '오버핏 맨투맨',       '그레이', 'L', 22000, TRUE),
(4,  2, 'CV-TOP-001', 'C 로고 티셔츠',       '네이비', 'M', 18000, TRUE),
(5,  2, 'CV-TOP-002', '스트라이프 셔츠',     '블루',   'L', 25000, TRUE),
(6,  3, 'TN-TOP-001', 'T-로고 후드',         '블랙',   'M', 35000, TRUE),
(7,  3, 'TN-TOP-002', '아치 로고 맨투맨',    '크림',   'L', 30000, TRUE),
(8,  4, 'MS-BOT-001', '와이드 데님 팬츠',    '인디고', 'M', 25000, TRUE),
(9,  4, 'MS-BOT-002', '슬랙스',              '블랙',   'L', 20000, TRUE),
(10, 5, 'SP-BOT-001', '쿨 조거팬츠',         '차콜',   'M', 12000, TRUE),
(11, 5, 'SP-BOT-002', '에어리 숏팬츠',       '카키',   'L', 10000, TRUE),
(12, 6, 'UQ-BOT-001', '에어리즘 이지팬츠',   '네이비', 'M', 15000, TRUE),
(13, 6, 'UQ-BOT-002', '스마트 앵클팬츠',     '그레이', 'L', 18000, TRUE),
(14, 7, 'NG-OUT-001', '칼리 플리스 자켓',    '아이보리','M', 55000, TRUE),
(15, 7, 'NG-OUT-002', '에코 패딩 점퍼',      '블랙',   'L', 80000, TRUE),
(16, 8, 'NF-OUT-001', '눕시 다운 자켓',      '블랙',   'M', 120000, TRUE),
(17, 8, 'NF-OUT-002', '화이트라벨 플리스',   '베이지', 'L', 45000, TRUE),
(18, 9, 'DC-OUT-001', '레스터 롱패딩',       '카키',   'M', 90000, TRUE),
(19, 9, 'DC-OUT-002', '버킷 플리스',         '브라운', 'L', 50000, TRUE);

-- 시퀀스 리셋
SELECT setval('product_categories_id_seq', 1);
SELECT setval('product_subcategories_id_seq', 3);
SELECT setval('brands_id_seq', 9);
SELECT setval('products_id_seq', 19);

-- ============================================================
-- 3. 재고 데이터
-- ============================================================
INSERT INTO inventory (sku, current_quantity, reorder_point, unit_cost, valuation_method, last_updated) VALUES
('MS-TOP-001', 350, 50, 15000, 'WEIGHTED_AVG', NOW()),
('MS-TOP-002', 280, 50, 15000, 'WEIGHTED_AVG', NOW()),
('MS-TOP-003', 200, 40, 22000, 'FIFO', NOW()),
('CV-TOP-001', 180, 30, 18000, 'WEIGHTED_AVG', NOW()),
('CV-TOP-002', 150, 25, 25000, 'FIFO', NOW()),
('TN-TOP-001', 120, 20, 35000, 'LIFO', NOW()),
('TN-TOP-002', 100, 20, 30000, 'LIFO', NOW()),
('MS-BOT-001', 250, 40, 25000, 'WEIGHTED_AVG', NOW()),
('MS-BOT-002', 220, 35, 20000, 'WEIGHTED_AVG', NOW()),
('SP-BOT-001', 400, 60, 12000, 'FIFO', NOW()),
('SP-BOT-002', 320, 50, 10000, 'FIFO', NOW()),
('UQ-BOT-001', 300, 45, 15000, 'WEIGHTED_AVG', NOW()),
('UQ-BOT-002', 270, 40, 18000, 'WEIGHTED_AVG', NOW()),
('NG-OUT-001', 80, 15, 55000, 'FIFO', NOW()),
('NG-OUT-002', 60, 10, 80000, 'FIFO', NOW()),
('NF-OUT-001', 45, 10, 120000, 'LIFO', NOW()),
('NF-OUT-002', 90, 15, 45000, 'LIFO', NOW()),
('DC-OUT-001', 55, 10, 90000, 'WEIGHTED_AVG', NOW()),
('DC-OUT-002', 75, 12, 50000, 'WEIGHTED_AVG', NOW());


-- ============================================================
-- 4. 판매 거래 데이터 (2024-01-01 ~ 2025-04-17)
--    계절별 패턴 반영, SKU별 현실적인 거래 생성
-- ============================================================
DO $$
DECLARE
    d DATE;
    sku_rec RECORD;
    seasonal_mult DOUBLE PRECISION;
    daily_tx_count INTEGER;
    i INTEGER;
    tx_hour INTEGER;
    tx_minute INTEGER;
    tx_time TIMESTAMP;
    qty INTEGER;
    unit_p DECIMAL(15,2);
    total_amt DECIMAL(15,2);
    disc_amt DECIMAL(15,2);
    ret_amt DECIMAL(15,2);
    is_bndl BOOLEAN;
    bndl_id INTEGER;
    ch TEXT;
    channels TEXT[] := ARRAY['온라인몰', '네이버스토어', '쿠팡', '무신사', '오프라인'];
    sku_codes TEXT[] := ARRAY[
        'MS-TOP-001','MS-TOP-002','MS-TOP-003',
        'CV-TOP-001','CV-TOP-002',
        'TN-TOP-001','TN-TOP-002',
        'MS-BOT-001','MS-BOT-002',
        'SP-BOT-001','SP-BOT-002',
        'UQ-BOT-001','UQ-BOT-002',
        'NG-OUT-001','NG-OUT-002',
        'NF-OUT-001','NF-OUT-002',
        'DC-OUT-001','DC-OUT-002'
    ];
    sku_prices DECIMAL[] := ARRAY[
        29900, 29900, 39900,
        35900, 49900,
        69900, 59900,
        49900, 39900,
        25900, 19900,
        29900, 39900,
        129000, 189000,
        289000, 99000,
        199000, 119000
    ];
    sku_idx INTEGER;
    rand_val DOUBLE PRECISION;
    month_val INTEGER;
BEGIN
    d := '2024-01-01'::DATE;
    WHILE d <= '2025-04-17'::DATE LOOP
        month_val := EXTRACT(MONTH FROM d);
        -- 계절별 배수
        seasonal_mult := CASE month_val
            WHEN 1 THEN 0.7
            WHEN 2 THEN 0.8
            WHEN 3 THEN 1.1
            WHEN 4 THEN 1.2
            WHEN 5 THEN 1.0
            WHEN 6 THEN 0.9
            WHEN 7 THEN 0.85
            WHEN 8 THEN 0.9
            WHEN 9 THEN 1.15
            WHEN 10 THEN 1.3
            WHEN 11 THEN 1.5
            WHEN 12 THEN 1.4
        END;

        -- 일별 거래 수 (30~80 * 계절배수)
        daily_tx_count := (30 + floor(random() * 51))::INTEGER;
        daily_tx_count := (daily_tx_count * seasonal_mult)::INTEGER;

        FOR i IN 1..daily_tx_count LOOP
            -- 랜덤 SKU 선택
            sku_idx := 1 + floor(random() * 19)::INTEGER;
            IF sku_idx > 19 THEN sku_idx := 19; END IF;

            unit_p := sku_prices[sku_idx];
            tx_hour := 9 + floor(random() * 15)::INTEGER;
            tx_minute := floor(random() * 60)::INTEGER;
            tx_time := d + (tx_hour || ' hours')::INTERVAL + (tx_minute || ' minutes')::INTERVAL;

            qty := 1 + floor(random() * 5)::INTEGER;
            total_amt := unit_p * qty;

            -- 10% 확률 할인
            disc_amt := 0;
            IF random() < 0.10 THEN
                disc_amt := ROUND((total_amt * (0.05 + random() * 0.25))::NUMERIC, 2);
            END IF;

            -- 3% 확률 반품
            ret_amt := 0;
            IF random() < 0.03 THEN
                ret_amt := total_amt;
            END IF;

            -- 5% 확률 번들
            is_bndl := random() < 0.05;
            bndl_id := NULL;
            IF is_bndl THEN
                bndl_id := 1000 + floor(random() * 9000)::INTEGER;
            END IF;

            ch := channels[1 + floor(random() * 5)::INTEGER];

            INSERT INTO sales_transactions (sku, transaction_time, quantity, unit_price, total_amount, discount_amount, return_amount, channel, is_bundle, bundle_id)
            VALUES (sku_codes[sku_idx], tx_time, qty, unit_p, total_amt, disc_amt, ret_amt, ch, is_bndl, bndl_id);
        END LOOP;

        d := d + INTERVAL '1 day';
    END LOOP;
END $$;


-- ============================================================
-- 5. 광고비 데이터 (2024-01-01 ~ 2025-04-17, 4개 채널)
-- ============================================================
DO $$
DECLARE
    d DATE;
    ch TEXT;
    ad_channels TEXT[] := ARRAY['NAVER', 'GOOGLE', 'META', 'OTHERS'];
    base_spend DECIMAL(15,2);
    spend DECIMAL(15,2);
    impr INTEGER;
    clk INTEGER;
    conv INTEGER;
    new_cust INTEGER;
    seasonal_mult DOUBLE PRECISION;
    month_val INTEGER;
BEGIN
    d := '2024-01-01'::DATE;
    WHILE d <= '2025-04-17'::DATE LOOP
        month_val := EXTRACT(MONTH FROM d);
        seasonal_mult := CASE month_val
            WHEN 1 THEN 0.7  WHEN 2 THEN 0.8  WHEN 3 THEN 1.1  WHEN 4 THEN 1.2
            WHEN 5 THEN 1.0  WHEN 6 THEN 0.9  WHEN 7 THEN 0.85 WHEN 8 THEN 0.9
            WHEN 9 THEN 1.15 WHEN 10 THEN 1.3 WHEN 11 THEN 1.5 WHEN 12 THEN 1.4
        END;

        FOREACH ch IN ARRAY ad_channels LOOP
            base_spend := CASE ch
                WHEN 'NAVER'  THEN 350000
                WHEN 'GOOGLE' THEN 250000
                WHEN 'META'   THEN 200000
                WHEN 'OTHERS' THEN 100000
            END;

            spend := ROUND((base_spend * seasonal_mult * (0.8 + random() * 0.4))::NUMERIC, 2);
            impr := (spend * (80 + floor(random() * 71)))::INTEGER;
            clk := (impr * (0.01 + random() * 0.04))::INTEGER;
            conv := (clk * (0.02 + random() * 0.06))::INTEGER;
            new_cust := GREATEST((conv * (0.3 + random() * 0.4))::INTEGER, 1);

            INSERT INTO advertising_costs (channel, cost_date, spend_amount, impressions, clicks, conversions, new_customers)
            VALUES (ch::ad_channel, d, spend, impr, clk, conv, new_cust);
        END LOOP;

        d := d + INTERVAL '1 day';
    END LOOP;
END $$;


-- ============================================================
-- 6. 일별 집계 데이터 (daily_aggregates)
--    sales_transactions + advertising_costs 기반으로 집계
-- ============================================================
INSERT INTO daily_aggregates (aggregate_date, sku, category, brand, total_revenue, net_revenue, cogs, gross_profit, sales_volume, ad_spend, roas, calculated_at)
SELECT
    st.tx_date AS aggregate_date,
    st.sku,
    '의류' AS category,
    CASE
        WHEN st.sku LIKE 'MS-%' THEN '무신사 스탠다드'
        WHEN st.sku LIKE 'CV-%' THEN '커버낫'
        WHEN st.sku LIKE 'TN-%' THEN '디스이즈네버댓'
        WHEN st.sku LIKE 'SP-%' THEN '스파오'
        WHEN st.sku LIKE 'UQ-%' THEN '유니클로'
        WHEN st.sku LIKE 'NG-%' THEN '내셔널지오그래픽'
        WHEN st.sku LIKE 'NF-%' THEN '노스페이스'
        WHEN st.sku LIKE 'DC-%' THEN '디스커버리'
    END AS brand,
    st.total_rev AS total_revenue,
    st.total_rev - st.total_disc - st.total_ret AS net_revenue,
    ROUND(((st.total_rev - st.total_disc - st.total_ret) * 0.45)::NUMERIC, 2) AS cogs,
    ROUND(((st.total_rev - st.total_disc - st.total_ret) * 0.55)::NUMERIC, 2) AS gross_profit,
    st.total_qty AS sales_volume,
    COALESCE(ad.daily_ad_spend, 0) AS ad_spend,
    CASE WHEN COALESCE(ad.daily_ad_spend, 0) > 0
         THEN ROUND((st.total_rev / ad.daily_ad_spend)::NUMERIC, 4)
         ELSE 0
    END AS roas,
    NOW() AS calculated_at
FROM (
    SELECT
        DATE(transaction_time) AS tx_date,
        sku,
        SUM(total_amount) AS total_rev,
        SUM(discount_amount) AS total_disc,
        SUM(return_amount) AS total_ret,
        SUM(quantity) AS total_qty
    FROM sales_transactions
    GROUP BY DATE(transaction_time), sku
) st
LEFT JOIN (
    SELECT cost_date, SUM(spend_amount) / 19.0 AS daily_ad_spend
    FROM advertising_costs
    GROUP BY cost_date
) ad ON st.tx_date = ad.cost_date
ORDER BY st.tx_date, st.sku;


-- ============================================================
-- 7. 월별 집계 데이터 (monthly_aggregates)
-- ============================================================
INSERT INTO monthly_aggregates (aggregate_month, category, brand, total_revenue, net_revenue, cogs, gross_profit, ebitda, operating_profit, net_profit, total_ad_spend, avg_roas, cac, budget_amount, calculated_at)
SELECT
    DATE_TRUNC('month', da.aggregate_date)::DATE AS aggregate_month,
    da.category,
    da.brand,
    SUM(da.total_revenue) AS total_revenue,
    SUM(da.net_revenue) AS net_revenue,
    SUM(da.cogs) AS cogs,
    SUM(da.gross_profit) AS gross_profit,
    ROUND((SUM(da.gross_profit) * 0.70)::NUMERIC, 2) AS ebitda,
    ROUND((SUM(da.gross_profit) * 0.70 * 0.95)::NUMERIC, 2) AS operating_profit,
    ROUND((SUM(da.gross_profit) * 0.70 * 0.95 * 0.75)::NUMERIC, 2) AS net_profit,
    SUM(da.ad_spend) AS total_ad_spend,
    CASE WHEN SUM(da.ad_spend) > 0
         THEN ROUND((SUM(da.total_revenue) / SUM(da.ad_spend))::NUMERIC, 4)
         ELSE 0
    END AS avg_roas,
    0 AS cac,
    ROUND((SUM(da.total_revenue) * 1.05)::NUMERIC, 2) AS budget_amount,
    NOW() AS calculated_at
FROM daily_aggregates da
GROUP BY DATE_TRUNC('month', da.aggregate_date), da.category, da.brand
ORDER BY aggregate_month, da.category, da.brand;

-- ============================================================
-- 8. 예산 목표 데이터 (budget_targets)
-- ============================================================
DO $$
DECLARE
    m DATE;
    base_rev DECIMAL(15,2);
    seasonal_mult DOUBLE PRECISION;
    month_val INTEGER;
BEGIN
    m := '2024-01-01'::DATE;
    WHILE m <= '2025-04-01'::DATE LOOP
        month_val := EXTRACT(MONTH FROM m);
        seasonal_mult := CASE month_val
            WHEN 1 THEN 0.7  WHEN 2 THEN 0.8  WHEN 3 THEN 1.1  WHEN 4 THEN 1.2
            WHEN 5 THEN 1.0  WHEN 6 THEN 0.9  WHEN 7 THEN 0.85 WHEN 8 THEN 0.9
            WHEN 9 THEN 1.15 WHEN 10 THEN 1.3 WHEN 11 THEN 1.5 WHEN 12 THEN 1.4
        END;

        -- 매출 목표 (월 1.5억 기준 * 계절배수)
        base_rev := 150000000 * seasonal_mult;
        INSERT INTO budget_targets (target_month, category, metric_type, target_value)
        VALUES (m, '의류', 'REVENUE', base_rev);

        -- 광고비 목표 (월 2700만 기준 * 계절배수)
        INSERT INTO budget_targets (target_month, category, metric_type, target_value)
        VALUES (m, '의류', 'AD_SPEND', 27000000 * seasonal_mult);

        -- 이익 목표 (매출의 약 25%)
        INSERT INTO budget_targets (target_month, category, metric_type, target_value)
        VALUES (m, '의류', 'PROFIT', base_rev * 0.25);

        m := m + INTERVAL '1 month';
    END LOOP;
END $$;

-- ============================================================
-- 9. 시스템 설정 데이터
-- ============================================================
INSERT INTO configurations (key, value, data_type, is_required) VALUES
('database.connection_string', 'jdbc:postgresql://localhost:5432/retail_dashboard', 'STRING', TRUE),
('api.endpoint', 'http://localhost:8080/mock/api', 'STRING', TRUE),
('api.auth.token', 'mock-api-secret-token-2024', 'STRING', TRUE),
('data.fetch.interval_minutes', '60', 'INTEGER', FALSE),
('data.retention.years', '5', 'INTEGER', FALSE),
('alert.revenue.threshold_percent', '90', 'DECIMAL', FALSE),
('alert.adspend.threshold_percent', '95', 'DECIMAL', FALSE),
('alert.anomaly.deviation_percent', '20', 'DECIMAL', FALSE),
('export.download_link.expiry_hours', '24', 'INTEGER', FALSE),
('report.history.retention_days', '90', 'INTEGER', FALSE);

-- ============================================================
-- 10. 알림 설정 (CEO 사용자 기본 설정)
-- ============================================================
INSERT INTO alert_configs (user_id, alert_type, threshold_value, severity, email_enabled, sms_enabled, push_enabled) VALUES
('a0000000-0000-0000-0000-000000000001', 'REVENUE', 90.00, 'CRITICAL', TRUE, TRUE, TRUE),
('a0000000-0000-0000-0000-000000000001', 'AD_SPEND', 95.00, 'IMPORTANT', TRUE, FALSE, TRUE),
('a0000000-0000-0000-0000-000000000001', 'ANOMALY', 20.00, 'IMPORTANT', TRUE, FALSE, FALSE),
('a0000000-0000-0000-0000-000000000001', 'INVENTORY', 0, 'INFORMATIONAL', TRUE, FALSE, FALSE),
('a0000000-0000-0000-0000-000000000004', 'REVENUE', 90.00, 'CRITICAL', TRUE, FALSE, TRUE),
('a0000000-0000-0000-0000-000000000004', 'AD_SPEND', 95.00, 'IMPORTANT', TRUE, FALSE, FALSE);

-- ============================================================
-- 11. 샘플 알림 데이터
-- ============================================================
INSERT INTO alerts (user_id, config_id, severity, title, message, is_acknowledged, triggered_at) VALUES
(NULL, NULL, 'CRITICAL', '매출 목표 미달 경고', '2025년 1월 매출이 목표 대비 85%에 그쳤습니다. 즉각적인 대응이 필요합니다.', FALSE, '2025-02-01 09:00:00'),
(NULL, NULL, 'IMPORTANT', '광고비 예산 초과 임박', 'META 채널 광고비가 월 예산의 96%에 도달했습니다.', TRUE, '2025-03-20 14:30:00'),
(NULL, NULL, 'INFORMATIONAL', '재고 부족 알림', 'NF-OUT-001 (눕시 다운 자켓) 재고가 재주문점 이하입니다. 현재 45개 / 재주문점 10개', FALSE, '2025-04-10 11:00:00'),
(NULL, NULL, 'IMPORTANT', '매출 이상 패턴 감지', '상의 카테고리 일 매출이 7일 이동평균 대비 25% 하락했습니다.', FALSE, '2025-04-15 08:00:00');

-- ============================================================
-- 12. 리포트 스케줄 데이터
-- ============================================================
INSERT INTO report_schedules (created_by, template_name, frequency, scheduled_time, recipients, is_active) VALUES
('a0000000-0000-0000-0000-000000000001', 'Monthly Executive Report', 'WEEKLY', '09:00:00',
 '{"emails": ["ceo@retaildashboard.com", "exec@retaildashboard.com"], "roles": ["CEO", "EXECUTIVE"]}', TRUE),
('a0000000-0000-0000-0000-000000000003', 'Weekly Marketing Report', 'WEEKLY', '08:00:00',
 '{"emails": ["marketing@retaildashboard.com"], "departments": ["MARKETING"]}', TRUE);

-- ============================================================
-- 13. 저장된 필터 데이터
-- ============================================================
INSERT INTO saved_filters (user_id, name, filter_criteria) VALUES
('a0000000-0000-0000-0000-000000000001', '전체 현황 (최근 30일)',
 '{"dateRange": {"from": "2025-03-18", "to": "2025-04-17"}, "category": null, "brand": null, "sku": null, "channel": null}'),
('a0000000-0000-0000-0000-000000000003', '무신사 스탠다드 상의',
 '{"dateRange": {"from": "2025-01-01", "to": "2025-04-17"}, "category": "의류", "brand": "무신사 스탠다드", "sku": null, "channel": null}'),
('a0000000-0000-0000-0000-000000000004', '아우터 브랜드 비교',
 '{"dateRange": {"from": "2024-10-01", "to": "2025-03-31"}, "category": "의류", "brand": null, "sku": null, "channel": null}');
