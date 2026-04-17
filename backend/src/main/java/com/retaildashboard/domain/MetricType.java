package com.retaildashboard.domain;

/**
 * 메트릭 유형 enum.
 * 비교 분석(YoY, MoM, 예산 대비)에서 사용되는 메트릭 유형을 정의합니다.
 *
 * Requirements: 4.11, 4.12, 4.13
 */
public enum MetricType {
    TOTAL_REVENUE,
    NET_REVENUE,
    GROSS_PROFIT,
    EBITDA,
    OPERATING_PROFIT,
    NET_PROFIT,
    AD_SPEND
}
