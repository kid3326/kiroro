package com.retaildashboard.domain;

/**
 * 데이터 접근 결과 정의.
 * PostgreSQL의 access_result ENUM 타입과 매핑됩니다.
 *
 * Requirements: 3.7, 14.7
 */
public enum AccessResult {
    /** 접근 허용 */
    GRANTED,

    /** 접근 거부 */
    DENIED
}
