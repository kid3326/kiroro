package com.retaildashboard.service;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mock API 클라이언트 구현체.
 * RestTemplate 기반으로 Mock API 엔드포인트를 호출하며,
 * RetryTemplate을 통한 재시도 로직과 응답 스키마 검증을 포함합니다.
 *
 * - 재시도: 최대 3회, 지수 백오프
 * - 스키마 검증: 필수 필드 null 체크 및 타입 검증
 * - 검증 실패 시: 에러 로깅 후 null 반환 (호출자가 캐시 데이터 사용)
 *
 * Requirements: 1.2, 1.4, 1.5, 1.6
 */
@Service
@Slf4j
public class MockApiClientImpl implements ApiClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public MockApiClientImpl(
            RestTemplate mockApiRestTemplate,
            RetryTemplate retryTemplate,
            @Value("${mock.api.base-url:http://localhost:8080}") String baseUrl) {
        this.restTemplate = mockApiRestTemplate;
        this.retryTemplate = retryTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public SalesDataResponse fetchSalesData(LocalDateTime from, LocalDateTime to) {
        String url = buildUrl("/mock/api/sales", from, to);
        log.info("판매 데이터 수집 시작: from={}, to={}", from, to);

        try {
            SalesDataResponse response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("판매 데이터 API 호출 시도 {}/3", attempt);
                return restTemplate.getForObject(url, SalesDataResponse.class);
            });

            if (!validateSalesData(response)) {
                log.error("판매 데이터 스키마 검증 실패 - 캐시 데이터를 사용합니다");
                return null;
            }

            log.info("판매 데이터 수집 완료: {}건", response.totalCount());
            return response;
        } catch (RestClientException e) {
            log.error("판매 데이터 수집 실패 (모든 재시도 소진): {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public AdvertisingCostResponse fetchAdvertisingCosts(LocalDateTime from, LocalDateTime to) {
        String url = buildUrl("/mock/api/costs", from, to);
        log.info("광고비 데이터 수집 시작: from={}, to={}", from, to);

        try {
            AdvertisingCostResponse response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("광고비 데이터 API 호출 시도 {}/3", attempt);
                return restTemplate.getForObject(url, AdvertisingCostResponse.class);
            });

            if (!validateAdvertisingCosts(response)) {
                log.error("광고비 데이터 스키마 검증 실패 - 캐시 데이터를 사용합니다");
                return null;
            }

            log.info("광고비 데이터 수집 완료: {}건", response.totalCount());
            return response;
        } catch (RestClientException e) {
            log.error("광고비 데이터 수집 실패 (모든 재시도 소진): {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public InventoryResponse fetchInventory() {
        String url = baseUrl + "/mock/api/inventory";
        log.info("재고 데이터 수집 시작");

        try {
            InventoryResponse response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("재고 데이터 API 호출 시도 {}/3", attempt);
                return restTemplate.getForObject(url, InventoryResponse.class);
            });

            if (!validateInventory(response)) {
                log.error("재고 데이터 스키마 검증 실패 - 캐시 데이터를 사용합니다");
                return null;
            }

            log.info("재고 데이터 수집 완료: {}건", response.totalCount());
            return response;
        } catch (RestClientException e) {
            log.error("재고 데이터 수집 실패 (모든 재시도 소진): {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public PnlMetricsResponse fetchPnlMetrics(LocalDateTime from, LocalDateTime to) {
        String url = buildUrl("/mock/api/pnl", from, to);
        log.info("P&L 메트릭 수집 시작: from={}, to={}", from, to);

        try {
            PnlMetricsResponse response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("P&L 메트릭 API 호출 시도 {}/3", attempt);
                return restTemplate.getForObject(url, PnlMetricsResponse.class);
            });

            if (!validatePnlMetrics(response)) {
                log.error("P&L 메트릭 스키마 검증 실패 - 캐시 데이터를 사용합니다");
                return null;
            }

            log.info("P&L 메트릭 수집 완료");
            return response;
        } catch (RestClientException e) {
            log.error("P&L 메트릭 수집 실패 (모든 재시도 소진): {}", e.getMessage(), e);
            return null;
        }
    }

    // === Schema Validation Methods ===

    /**
     * 판매 데이터 응답 스키마를 검증합니다.
     * 필수 필드가 null이 아니고 올바른 타입인지 확인합니다.
     */
    boolean validateSalesData(SalesDataResponse response) {
        if (response == null) {
            log.warn("판매 데이터 응답이 null입니다");
            return false;
        }
        if (response.transactions() == null) {
            log.warn("판매 데이터 transactions 필드가 null입니다");
            return false;
        }
        for (SalesDataResponse.SalesTransaction tx : response.transactions()) {
            if (tx.sku() == null || tx.sku().isBlank()) {
                log.warn("판매 거래의 SKU가 null 또는 빈 문자열입니다");
                return false;
            }
            if (tx.transactionTime() == null) {
                log.warn("판매 거래의 transactionTime이 null입니다: sku={}", tx.sku());
                return false;
            }
            if (tx.unitPrice() == null || tx.totalAmount() == null) {
                log.warn("판매 거래의 가격 정보가 null입니다: sku={}", tx.sku());
                return false;
            }
        }
        return true;
    }

    /**
     * 광고비 데이터 응답 스키마를 검증합니다.
     */
    boolean validateAdvertisingCosts(AdvertisingCostResponse response) {
        if (response == null) {
            log.warn("광고비 데이터 응답이 null입니다");
            return false;
        }
        if (response.costs() == null) {
            log.warn("광고비 데이터 costs 필드가 null입니다");
            return false;
        }
        for (AdvertisingCostResponse.AdvertisingCost cost : response.costs()) {
            if (cost.channel() == null || cost.channel().isBlank()) {
                log.warn("광고비의 channel이 null 또는 빈 문자열입니다");
                return false;
            }
            if (cost.costDate() == null) {
                log.warn("광고비의 costDate가 null입니다: channel={}", cost.channel());
                return false;
            }
            if (cost.spendAmount() == null) {
                log.warn("광고비의 spendAmount가 null입니다: channel={}", cost.channel());
                return false;
            }
        }
        return true;
    }

    /**
     * 재고 데이터 응답 스키마를 검증합니다.
     */
    boolean validateInventory(InventoryResponse response) {
        if (response == null) {
            log.warn("재고 데이터 응답이 null입니다");
            return false;
        }
        if (response.items() == null) {
            log.warn("재고 데이터 items 필드가 null입니다");
            return false;
        }
        for (InventoryResponse.InventoryItem item : response.items()) {
            if (item.sku() == null || item.sku().isBlank()) {
                log.warn("재고 항목의 SKU가 null 또는 빈 문자열입니다");
                return false;
            }
            if (item.unitCost() == null) {
                log.warn("재고 항목의 unitCost가 null입니다: sku={}", item.sku());
                return false;
            }
            if (item.lastUpdated() == null) {
                log.warn("재고 항목의 lastUpdated가 null입니다: sku={}", item.sku());
                return false;
            }
        }
        return true;
    }

    /**
     * P&L 메트릭 응답 스키마를 검증합니다.
     */
    boolean validatePnlMetrics(PnlMetricsResponse response) {
        if (response == null) {
            log.warn("P&L 메트릭 응답이 null입니다");
            return false;
        }
        if (response.totalRevenue() == null || response.netRevenue() == null) {
            log.warn("P&L 메트릭의 매출 필드가 null입니다");
            return false;
        }
        if (response.cogs() == null || response.grossProfit() == null) {
            log.warn("P&L 메트릭의 비용/이익 필드가 null입니다");
            return false;
        }
        if (response.ebitda() == null || response.operatingProfit() == null || response.netProfit() == null) {
            log.warn("P&L 메트릭의 이익 지표 필드가 null입니다");
            return false;
        }
        if (response.advertisingMetrics() == null) {
            log.warn("P&L 메트릭의 advertisingMetrics가 null입니다");
            return false;
        }
        if (response.categoryBreakdown() == null) {
            log.warn("P&L 메트릭의 categoryBreakdown이 null입니다");
            return false;
        }
        return true;
    }

    // === Private Helper Methods ===

    /**
     * 날짜 범위 쿼리 파라미터를 포함한 URL을 생성합니다.
     */
    private String buildUrl(String path, LocalDateTime from, LocalDateTime to) {
        return String.format("%s%s?from=%s&to=%s",
                baseUrl, path,
                from.toLocalDate().format(DATE_FORMATTER),
                to.toLocalDate().format(DATE_FORMATTER));
    }
}
