package com.retaildashboard.dto.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mock API 광고비 데이터 응답 DTO.
 * /mock/api/costs 엔드포인트에서 반환됩니다.
 *
 * Requirements: 1.1
 */
public record AdvertisingCostResponse(
        List<AdvertisingCost> costs,
        int totalCount,
        String fromDate,
        String toDate
) {
    public record AdvertisingCost(
            String channel,
            LocalDate costDate,
            BigDecimal spendAmount,
            int impressions,
            int clicks,
            int conversions,
            int newCustomers
    ) {}
}
