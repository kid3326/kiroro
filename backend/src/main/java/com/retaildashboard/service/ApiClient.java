package com.retaildashboard.service;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;

import java.time.LocalDateTime;

/**
 * API 클라이언트 인터페이스.
 * Mock API로부터 데이터를 수집하는 계약을 정의합니다.
 *
 * 구현체는 재시도 로직, 스키마 검증, 에러 처리를 포함해야 합니다.
 *
 * Requirements: 1.2, 1.4, 1.5, 1.6
 */
public interface ApiClient {

    /**
     * 판매 데이터를 조회합니다.
     *
     * @param from 시작 일시
     * @param to   종료 일시
     * @return 판매 데이터 응답, 검증 실패 시 null
     */
    SalesDataResponse fetchSalesData(LocalDateTime from, LocalDateTime to);

    /**
     * 광고비 데이터를 조회합니다.
     *
     * @param from 시작 일시
     * @param to   종료 일시
     * @return 광고비 데이터 응답, 검증 실패 시 null
     */
    AdvertisingCostResponse fetchAdvertisingCosts(LocalDateTime from, LocalDateTime to);

    /**
     * 재고 데이터를 조회합니다.
     *
     * @return 재고 데이터 응답, 검증 실패 시 null
     */
    InventoryResponse fetchInventory();

    /**
     * P&L 메트릭을 조회합니다.
     *
     * @param from 시작 일시
     * @param to   종료 일시
     * @return P&L 메트릭 응답, 검증 실패 시 null
     */
    PnlMetricsResponse fetchPnlMetrics(LocalDateTime from, LocalDateTime to);
}
