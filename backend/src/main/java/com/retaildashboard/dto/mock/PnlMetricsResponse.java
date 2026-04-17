package com.retaildashboard.dto.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Mock API P&L 메트릭 응답 DTO.
 * /mock/api/pnl 엔드포인트에서 반환됩니다.
 *
 * Requirements: 1.1
 */
public record PnlMetricsResponse(
        BigDecimal totalRevenue,
        BigDecimal netRevenue,
        BigDecimal cogs,
        BigDecimal grossProfit,
        BigDecimal ebitda,
        BigDecimal operatingProfit,
        BigDecimal netProfit,
        BigDecimal grossMarginPercent,
        BigDecimal operatingMarginPercent,
        BigDecimal netMarginPercent,
        Map<String, ChannelAdMetrics> advertisingMetrics,
        List<CategoryBreakdown> categoryBreakdown,
        String fromDate,
        String toDate
) {
    public record ChannelAdMetrics(
            BigDecimal spend,
            BigDecimal revenue,
            BigDecimal roas,
            int conversions,
            int newCustomers,
            BigDecimal cac
    ) {}

    public record CategoryBreakdown(
            String category,
            BigDecimal revenue,
            BigDecimal cogs,
            BigDecimal grossProfit,
            int salesVolume
    ) {}
}
