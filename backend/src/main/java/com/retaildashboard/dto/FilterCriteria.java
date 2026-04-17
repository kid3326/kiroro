package com.retaildashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 필터 조건 DTO.
 * 대시보드 데이터 조회 시 적용할 필터 조건을 정의합니다.
 *
 * Requirements: 7.1, 7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {

    /** 상품 카테고리 */
    private String category;

    /** 브랜드 */
    private String brand;

    /** SKU */
    private String sku;

    /** 광고 채널 (NAVER, GOOGLE, META, OTHERS) */
    private String channel;
}
