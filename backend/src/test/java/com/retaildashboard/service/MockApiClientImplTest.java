package com.retaildashboard.service;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MockApiClientImpl 단위 테스트.
 * 재시도 로직, 스키마 검증, 에러 처리를 검증합니다.
 *
 * Requirements: 1.2, 1.4, 1.5, 1.6
 */
@ExtendWith(MockitoExtension.class)
class MockApiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private MockApiClientImpl apiClient;

    private static final String BASE_URL = "http://localhost:8080";
    private static final LocalDateTime FROM = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2025, 1, 31, 23, 59);

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(10L); // 테스트에서는 짧은 간격 사용
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(100L);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        apiClient = new MockApiClientImpl(restTemplate, retryTemplate, BASE_URL);
    }

    @Nested
    @DisplayName("fetchSalesData")
    class FetchSalesDataTests {

        @Test
        @DisplayName("정상 응답 시 판매 데이터를 반환한다")
        void shouldReturnSalesDataOnSuccess() {
            SalesDataResponse expected = new SalesDataResponse(
                    List.of(new SalesDataResponse.SalesTransaction(
                            "MS-TOP-001", LocalDateTime.now(), 2,
                            new BigDecimal("29900"), new BigDecimal("59800"),
                            BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                    1, "2025-01-01", "2025-01-31");

            when(restTemplate.getForObject(anyString(), eq(SalesDataResponse.class)))
                    .thenReturn(expected);

            SalesDataResponse result = apiClient.fetchSalesData(FROM, TO);

            assertThat(result).isNotNull();
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.transactions()).hasSize(1);
        }

        @Test
        @DisplayName("API 에러 시 3회 재시도 후 null을 반환한다")
        void shouldRetryAndReturnNullOnFailure() {
            when(restTemplate.getForObject(anyString(), eq(SalesDataResponse.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            SalesDataResponse result = apiClient.fetchSalesData(FROM, TO);

            assertThat(result).isNull();
            verify(restTemplate, times(3)).getForObject(anyString(), eq(SalesDataResponse.class));
        }

        @Test
        @DisplayName("2번째 시도에서 성공하면 데이터를 반환한다")
        void shouldReturnDataOnSecondAttempt() {
            SalesDataResponse expected = new SalesDataResponse(
                    List.of(new SalesDataResponse.SalesTransaction(
                            "MS-TOP-001", LocalDateTime.now(), 1,
                            new BigDecimal("29900"), new BigDecimal("29900"),
                            BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                    1, "2025-01-01", "2025-01-31");

            when(restTemplate.getForObject(anyString(), eq(SalesDataResponse.class)))
                    .thenThrow(new RestClientException("Timeout"))
                    .thenReturn(expected);

            SalesDataResponse result = apiClient.fetchSalesData(FROM, TO);

            assertThat(result).isNotNull();
            verify(restTemplate, times(2)).getForObject(anyString(), eq(SalesDataResponse.class));
        }
    }

    @Nested
    @DisplayName("fetchAdvertisingCosts")
    class FetchAdvertisingCostsTests {

        @Test
        @DisplayName("정상 응답 시 광고비 데이터를 반환한다")
        void shouldReturnCostsOnSuccess() {
            AdvertisingCostResponse expected = new AdvertisingCostResponse(
                    List.of(new AdvertisingCostResponse.AdvertisingCost(
                            "NAVER", LocalDate.of(2025, 1, 1),
                            new BigDecimal("350000"), 50000, 500, 25, 10)),
                    1, "2025-01-01", "2025-01-31");

            when(restTemplate.getForObject(anyString(), eq(AdvertisingCostResponse.class)))
                    .thenReturn(expected);

            AdvertisingCostResponse result = apiClient.fetchAdvertisingCosts(FROM, TO);

            assertThat(result).isNotNull();
            assertThat(result.costs()).hasSize(1);
        }

        @Test
        @DisplayName("API 에러 시 null을 반환한다")
        void shouldReturnNullOnFailure() {
            when(restTemplate.getForObject(anyString(), eq(AdvertisingCostResponse.class)))
                    .thenThrow(new RestClientException("Server error"));

            AdvertisingCostResponse result = apiClient.fetchAdvertisingCosts(FROM, TO);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("fetchInventory")
    class FetchInventoryTests {

        @Test
        @DisplayName("정상 응답 시 재고 데이터를 반환한다")
        void shouldReturnInventoryOnSuccess() {
            InventoryResponse expected = new InventoryResponse(
                    List.of(new InventoryResponse.InventoryItem(
                            "MS-TOP-001", 100, 20,
                            new BigDecimal("15000"), "FIFO", LocalDateTime.now())),
                    1);

            when(restTemplate.getForObject(anyString(), eq(InventoryResponse.class)))
                    .thenReturn(expected);

            InventoryResponse result = apiClient.fetchInventory();

            assertThat(result).isNotNull();
            assertThat(result.items()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("fetchPnlMetrics")
    class FetchPnlMetricsTests {

        @Test
        @DisplayName("정상 응답 시 P&L 메트릭을 반환한다")
        void shouldReturnPnlMetricsOnSuccess() {
            PnlMetricsResponse expected = createValidPnlResponse();

            when(restTemplate.getForObject(anyString(), eq(PnlMetricsResponse.class)))
                    .thenReturn(expected);

            PnlMetricsResponse result = apiClient.fetchPnlMetrics(FROM, TO);

            assertThat(result).isNotNull();
            assertThat(result.totalRevenue()).isEqualTo(new BigDecimal("100000000"));
        }
    }

    @Nested
    @DisplayName("스키마 검증")
    class SchemaValidationTests {

        @Test
        @DisplayName("판매 데이터 응답이 null이면 검증 실패")
        void salesDataNullResponseFails() {
            assertThat(apiClient.validateSalesData(null)).isFalse();
        }

        @Test
        @DisplayName("판매 데이터 transactions가 null이면 검증 실패")
        void salesDataNullTransactionsFails() {
            SalesDataResponse response = new SalesDataResponse(null, 0, "2025-01-01", "2025-01-31");
            assertThat(apiClient.validateSalesData(response)).isFalse();
        }

        @Test
        @DisplayName("판매 거래의 SKU가 빈 문자열이면 검증 실패")
        void salesDataEmptySkuFails() {
            SalesDataResponse response = new SalesDataResponse(
                    List.of(new SalesDataResponse.SalesTransaction(
                            "", LocalDateTime.now(), 1,
                            new BigDecimal("29900"), new BigDecimal("29900"),
                            BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                    1, "2025-01-01", "2025-01-31");
            assertThat(apiClient.validateSalesData(response)).isFalse();
        }

        @Test
        @DisplayName("유효한 판매 데이터는 검증 성공")
        void validSalesDataPasses() {
            SalesDataResponse response = new SalesDataResponse(
                    List.of(new SalesDataResponse.SalesTransaction(
                            "MS-TOP-001", LocalDateTime.now(), 1,
                            new BigDecimal("29900"), new BigDecimal("29900"),
                            BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                    1, "2025-01-01", "2025-01-31");
            assertThat(apiClient.validateSalesData(response)).isTrue();
        }

        @Test
        @DisplayName("광고비 데이터 응답이 null이면 검증 실패")
        void advertisingCostsNullResponseFails() {
            assertThat(apiClient.validateAdvertisingCosts(null)).isFalse();
        }

        @Test
        @DisplayName("재고 데이터 응답이 null이면 검증 실패")
        void inventoryNullResponseFails() {
            assertThat(apiClient.validateInventory(null)).isFalse();
        }

        @Test
        @DisplayName("P&L 메트릭 응답이 null이면 검증 실패")
        void pnlMetricsNullResponseFails() {
            assertThat(apiClient.validatePnlMetrics(null)).isFalse();
        }

        @Test
        @DisplayName("P&L 메트릭의 필수 필드가 null이면 검증 실패")
        void pnlMetricsMissingFieldsFails() {
            PnlMetricsResponse response = new PnlMetricsResponse(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, "2025-01-01", "2025-01-31");
            assertThat(apiClient.validatePnlMetrics(response)).isFalse();
        }

        @Test
        @DisplayName("스키마 검증 실패 시 fetchSalesData는 null을 반환한다")
        void fetchSalesDataReturnsNullOnValidationFailure() {
            SalesDataResponse invalidResponse = new SalesDataResponse(null, 0, "2025-01-01", "2025-01-31");

            when(restTemplate.getForObject(anyString(), eq(SalesDataResponse.class)))
                    .thenReturn(invalidResponse);

            SalesDataResponse result = apiClient.fetchSalesData(FROM, TO);

            assertThat(result).isNull();
        }
    }

    // === Helper Methods ===

    private PnlMetricsResponse createValidPnlResponse() {
        return new PnlMetricsResponse(
                new BigDecimal("100000000"),
                new BigDecimal("92000000"),
                new BigDecimal("41400000"),
                new BigDecimal("50600000"),
                new BigDecimal("35420000"),
                new BigDecimal("33649000"),
                new BigDecimal("25236750"),
                new BigDecimal("55.00"),
                new BigDecimal("36.57"),
                new BigDecimal("27.43"),
                Map.of("NAVER", new PnlMetricsResponse.ChannelAdMetrics(
                        new BigDecimal("10500000"), new BigDecimal("31500000"),
                        new BigDecimal("3.00"), 2100, 700, new BigDecimal("15000"))),
                List.of(new PnlMetricsResponse.CategoryBreakdown(
                        "상의", new BigDecimal("40000000"), new BigDecimal("18000000"),
                        new BigDecimal("22000000"), 800)),
                "2025-01-01", "2025-01-31");
    }
}
