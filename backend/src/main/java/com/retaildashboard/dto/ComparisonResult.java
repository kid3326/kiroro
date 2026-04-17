package com.retaildashboard.dto;

import java.math.BigDecimal;

/**
 * 비교 분석 결과 DTO.
 * YoY, MoM, 예산 대비 실적 비교 결과를 담습니다.
 *
 * @param currentValue  현재 기간 값
 * @param previousValue 비교 기간 값
 * @param difference    차이 (현재 - 비교)
 * @param changeRate    변화율 (%)
 *
 * Requirements: 4.11, 4.12, 4.13
 */
public record ComparisonResult(
        BigDecimal currentValue,
        BigDecimal previousValue,
        BigDecimal difference,
        BigDecimal changeRate
) {
}
