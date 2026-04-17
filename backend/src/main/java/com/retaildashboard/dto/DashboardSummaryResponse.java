package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * P&L 요약 메트릭 응답 DTO.
 * 대시보드 요약 화면에 표시되는 주요 재무 지표를 담습니다.
 *
 * Requirements: 6.1, 4.1, 4.2, 4.4, 4.8, 4.9, 4.10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    /** 전체 매출 */
    private BigDecimal totalRevenue;

    /** 순매출 (총매출 - 반품 - 할인) */
    private BigDecimal netRevenue;

    /** 매출총이익 (순매출 - 매출원가) */
    private BigDecimal grossProfit;

    /** EBITDA */
    private BigDecimal ebitda;

    /** 영업이익 */
    private BigDecimal operatingProfit;

    /** 순이익 */
    private BigDecimal netProfit;

    /** 전체 광고비 */
    private BigDecimal totalAdSpend;

    /** 조회 시작일 */
    private String fromDate;

    /** 조회 종료일 */
    private String toDate;
}
