package com.retaildashboard.controller.mock;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import com.retaildashboard.service.MockDataGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Mock API 컨트롤러.
 * 외부 API를 시뮬레이션하여 판매, 광고비, 재고, P&L 데이터를 제공합니다.
 *
 * 토큰 기반 인증: Authorization 헤더에 Bearer 토큰 필요.
 * 날짜 범위: from, to 쿼리 파라미터로 조회 기간 설정 (기본 최근 30일).
 *
 * Requirements: 1.1, 1.2
 */
@RestController
@RequestMapping("/mock/api")
@RequiredArgsConstructor
@Slf4j
public class MockApiController {

    private final MockDataGenerator mockDataGenerator;

    @Value("${mock.api.token:mock-api-secret-token-2024}")
    private String expectedToken;

    /**
     * 판매 데이터 조회.
     *
     * @param from 시작 날짜 (ISO 8601, 기본: 30일 전)
     * @param to   종료 날짜 (ISO 8601, 기본: 오늘)
     * @return 판매 거래 데이터
     */
    @GetMapping("/sales")
    public ResponseEntity<?> getSalesData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        ResponseEntity<?> authError = validateToken(authHeader);
        if (authError != null) return authError;

        LocalDate[] dateRange = parseDateRange(from, to);
        if (dateRange == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid date format. Use ISO 8601 (yyyy-MM-dd)"));
        }

        log.info("Mock API: 판매 데이터 요청 from={} to={}", dateRange[0], dateRange[1]);
        SalesDataResponse response = mockDataGenerator.generateSalesData(dateRange[0], dateRange[1]);
        return ResponseEntity.ok(response);
    }

    /**
     * 광고비 데이터 조회.
     *
     * @param from 시작 날짜 (ISO 8601, 기본: 30일 전)
     * @param to   종료 날짜 (ISO 8601, 기본: 오늘)
     * @return 채널별 광고비 데이터
     */
    @GetMapping("/costs")
    public ResponseEntity<?> getAdvertisingCosts(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        ResponseEntity<?> authError = validateToken(authHeader);
        if (authError != null) return authError;

        LocalDate[] dateRange = parseDateRange(from, to);
        if (dateRange == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid date format. Use ISO 8601 (yyyy-MM-dd)"));
        }

        log.info("Mock API: 광고비 데이터 요청 from={} to={}", dateRange[0], dateRange[1]);
        AdvertisingCostResponse response = mockDataGenerator.generateAdvertisingCosts(dateRange[0], dateRange[1]);
        return ResponseEntity.ok(response);
    }

    /**
     * 재고 데이터 조회.
     *
     * @return 전체 SKU 재고 현황
     */
    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ResponseEntity<?> authError = validateToken(authHeader);
        if (authError != null) return authError;

        log.info("Mock API: 재고 데이터 요청");
        InventoryResponse response = mockDataGenerator.generateInventory();
        return ResponseEntity.ok(response);
    }

    /**
     * P&L 메트릭 조회.
     *
     * @param from 시작 날짜 (ISO 8601, 기본: 30일 전)
     * @param to   종료 날짜 (ISO 8601, 기본: 오늘)
     * @return P&L 종합 메트릭
     */
    @GetMapping("/pnl")
    public ResponseEntity<?> getPnlMetrics(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        ResponseEntity<?> authError = validateToken(authHeader);
        if (authError != null) return authError;

        LocalDate[] dateRange = parseDateRange(from, to);
        if (dateRange == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid date format. Use ISO 8601 (yyyy-MM-dd)"));
        }

        log.info("Mock API: P&L 메트릭 요청 from={} to={}", dateRange[0], dateRange[1]);
        PnlMetricsResponse response = mockDataGenerator.generatePnlMetrics(dateRange[0], dateRange[1]);
        return ResponseEntity.ok(response);
    }

    // === Private Helper Methods ===

    /**
     * Bearer 토큰 검증.
     * Authorization 헤더에서 "Bearer {token}" 형식의 토큰을 추출하고 검증합니다.
     *
     * @return 인증 실패 시 에러 ResponseEntity, 성공 시 null
     */
    private ResponseEntity<?> validateToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Authorization header is required"));
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Authorization header must use Bearer scheme"));
        }

        String token = authHeader.substring(7).trim();
        if (!expectedToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid API token"));
        }

        return null;
    }

    /**
     * 날짜 범위 파싱.
     * from/to 파라미터가 없으면 기본값(최근 30일)을 사용합니다.
     *
     * @return [from, to] 배열, 파싱 실패 시 null
     */
    private LocalDate[] parseDateRange(String from, String to) {
        try {
            LocalDate fromDate = (from != null && !from.isBlank())
                    ? LocalDate.parse(from)
                    : LocalDate.now().minusDays(30);
            LocalDate toDate = (to != null && !to.isBlank())
                    ? LocalDate.parse(to)
                    : LocalDate.now();
            return new LocalDate[]{fromDate, toDate};
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
