package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 상품 메트릭 DTO.
 * 상품 계층별 집계 결과를 담습니다.
 *
 * Requirements: 5.1, 5.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMetrics {

    /** 계층 레벨 (CATEGORY, SUBCATEGORY, BRAND, SKU) */
    private String hierarchyLevel;

    /** 계층 이름 (카테고리명, 브랜드명, SKU 등) */
    private String name;

    /** 판매량 */
    private Long salesVolume;

    /** 매출 */
    private BigDecimal revenue;

    /** 변형 색상 (SKU 레벨에서만 사용) */
    private String variantColor;

    /** 변형 사이즈 (SKU 레벨에서만 사용) */
    private String variantSize;
}
