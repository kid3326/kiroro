package com.retaildashboard.domain;

/**
 * 데이터 유형 정의.
 * 역할 기반 접근 제어에서 데이터 접근 권한을 판단하는 데 사용됩니다.
 *
 * Requirements: 3.2, 3.3, 3.4, 3.5
 */
public enum DataType {
    /** 매출 데이터 - 전체 역할 접근 허용 */
    SALES,

    /** 비용 데이터 - CEO, Executive, Finance만 접근 허용 */
    COSTS,

    /** 광고비 데이터 - CEO, Executive, Finance, Marketing 접근 허용 */
    ADVERTISING,

    /** 재고 데이터 - 전체 역할 접근 허용 */
    INVENTORY,

    /** 상품 데이터 - 전체 역할 접근 허용 */
    PRODUCT
}
