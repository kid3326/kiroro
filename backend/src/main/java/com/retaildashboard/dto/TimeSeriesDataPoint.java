package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시계열 데이터 포인트 DTO.
 * 일별/주별/월별 시계열 차트에 사용되는 데이터 포인트를 담습니다.
 *
 * Requirements: 6.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {

    /** 날짜 */
    private LocalDate date;

    /** 전체 매출 */
    private BigDecimal totalRevenue;

    /** 순매출 */
    private BigDecimal netRevenue;

    /** 매출원가 */
    private BigDecimal cogs;

    /** 매출총이익 */
    private BigDecimal grossProfit;

    /** 판매량 */
    private Integer salesVolume;

    /** 광고비 */
    private BigDecimal adSpend;

    /** ROAS */
    private BigDecimal roas;
}
