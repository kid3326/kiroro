package com.retaildashboard.dto.mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock API 판매 데이터 응답 DTO.
 * /mock/api/sales 엔드포인트에서 반환됩니다.
 *
 * Requirements: 1.1
 */
public record SalesDataResponse(
        List<SalesTransaction> transactions,
        int totalCount,
        String fromDate,
        String toDate
) {
    public record SalesTransaction(
            String sku,
            LocalDateTime transactionTime,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal returnAmount,
            String channel,
            boolean isBundle,
            Integer bundleId
    ) {}
}
