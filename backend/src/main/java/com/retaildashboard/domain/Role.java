package com.retaildashboard.domain;

/**
 * 사용자 역할 정의.
 * PostgreSQL의 user_role ENUM 타입과 매핑됩니다.
 */
public enum Role {
    CEO,
    EXECUTIVE,
    MARKETING,
    FINANCE,
    PRODUCT
}
