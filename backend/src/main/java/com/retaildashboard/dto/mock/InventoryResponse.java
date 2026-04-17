package com.retaildashboard.dto.mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock API 재고 데이터 응답 DTO.
 * /mock/api/inventory 엔드포인트에서 반환됩니다.
 *
 * Requirements: 1.1
 */
public record InventoryResponse(
        List<InventoryItem> items,
        int totalCount
) {
    public record InventoryItem(
            String sku,
            int currentQuantity,
            int reorderPoint,
            BigDecimal unitCost,
            String valuationMethod,
            LocalDateTime lastUpdated
    ) {}
}
