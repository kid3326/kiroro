package com.retaildashboard.domain;

/**
 * 재고 평가 방법 enum.
 * COGS 계산 시 사용되는 재고 평가법을 정의합니다.
 *
 * Requirements: 4.3
 */
public enum InventoryValuationMethod {
    /** 선입선출법 */
    FIFO,
    /** 후입선출법 */
    LIFO,
    /** 가중평균법 */
    WEIGHTED_AVG
}
