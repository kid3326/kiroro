package com.retaildashboard.service;

import com.retaildashboard.domain.AdvertisingCost;
import com.retaildashboard.domain.Inventory;
import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import com.retaildashboard.repository.AdvertisingCostRepository;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DataCollectionScheduler 단위 테스트.
 * 데이터 수집, 저장, 에러 처리를 검증합니다.
 *
 * Requirements: 1.3
 */
@ExtendWith(MockitoExtension.class)
class DataCollectionSchedulerTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private AdvertisingCostRepository advertisingCostRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private DataCollectionScheduler scheduler;

    private static final LocalDateTime FROM = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2025, 1, 1, 1, 0);

    @Test
    @DisplayName("판매 데이터 수집 성공 시 DB에 저장한다")
    void shouldSaveSalesDataOnSuccess() {
        SalesDataResponse response = new SalesDataResponse(
                List.of(new SalesDataResponse.SalesTransaction(
                        "MS-TOP-001", LocalDateTime.now(), 2,
                        new BigDecimal("29900"), new BigDecimal("59800"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                1, "2025-01-01", "2025-01-01");

        when(apiClient.fetchSalesData(any(), any())).thenReturn(response);

        boolean result = scheduler.collectSalesData(FROM, TO);

        assertThat(result).isTrue();
        verify(salesTransactionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("판매 데이터 API 응답이 null이면 저장하지 않는다")
    void shouldNotSaveSalesDataWhenResponseIsNull() {
        when(apiClient.fetchSalesData(any(), any())).thenReturn(null);

        boolean result = scheduler.collectSalesData(FROM, TO);

        assertThat(result).isFalse();
        verify(salesTransactionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("광고비 데이터 수집 성공 시 DB에 저장한다")
    void shouldSaveAdvertisingCostsOnSuccess() {
        AdvertisingCostResponse response = new AdvertisingCostResponse(
                List.of(new AdvertisingCostResponse.AdvertisingCost(
                        "NAVER", LocalDate.of(2025, 1, 1),
                        new BigDecimal("350000"), 50000, 500, 25, 10)),
                1, "2025-01-01", "2025-01-01");

        when(apiClient.fetchAdvertisingCosts(any(), any())).thenReturn(response);

        boolean result = scheduler.collectAdvertisingCosts(FROM, TO);

        assertThat(result).isTrue();
        verify(advertisingCostRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("광고비 데이터 API 응답이 null이면 저장하지 않는다")
    void shouldNotSaveCostsWhenResponseIsNull() {
        when(apiClient.fetchAdvertisingCosts(any(), any())).thenReturn(null);

        boolean result = scheduler.collectAdvertisingCosts(FROM, TO);

        assertThat(result).isFalse();
        verify(advertisingCostRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("재고 데이터 수집 시 기존 SKU는 업데이트한다")
    void shouldUpdateExistingInventory() {
        InventoryResponse response = new InventoryResponse(
                List.of(new InventoryResponse.InventoryItem(
                        "MS-TOP-001", 200, 30,
                        new BigDecimal("15000"), "FIFO", LocalDateTime.now())),
                1);

        Inventory existing = Inventory.builder()
                .id(1)
                .sku("MS-TOP-001")
                .currentQuantity(100)
                .reorderPoint(20)
                .unitCost(new BigDecimal("15000"))
                .valuationMethod("WEIGHTED_AVG")
                .lastUpdated(LocalDateTime.now().minusDays(1))
                .build();

        when(apiClient.fetchInventory()).thenReturn(response);
        when(inventoryRepository.findBySku("MS-TOP-001")).thenReturn(Optional.of(existing));

        boolean result = scheduler.collectInventoryData();

        assertThat(result).isTrue();
        verify(inventoryRepository).save(any(Inventory.class));
        assertThat(existing.getCurrentQuantity()).isEqualTo(200);
        assertThat(existing.getValuationMethod()).isEqualTo("FIFO");
    }

    @Test
    @DisplayName("재고 데이터 수집 시 새 SKU는 생성한다")
    void shouldCreateNewInventory() {
        InventoryResponse response = new InventoryResponse(
                List.of(new InventoryResponse.InventoryItem(
                        "NEW-SKU-001", 50, 10,
                        new BigDecimal("20000"), "LIFO", LocalDateTime.now())),
                1);

        when(apiClient.fetchInventory()).thenReturn(response);
        when(inventoryRepository.findBySku("NEW-SKU-001")).thenReturn(Optional.empty());

        boolean result = scheduler.collectInventoryData();

        assertThat(result).isTrue();
        verify(inventoryRepository).save(argThat(inv ->
                inv.getSku().equals("NEW-SKU-001") && inv.getCurrentQuantity() == 50));
    }

    @Test
    @DisplayName("재고 데이터 API 응답이 null이면 저장하지 않는다")
    void shouldNotSaveInventoryWhenResponseIsNull() {
        when(apiClient.fetchInventory()).thenReturn(null);

        boolean result = scheduler.collectInventoryData();

        assertThat(result).isFalse();
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("판매 데이터 저장 중 예외 발생 시 false를 반환한다")
    void shouldReturnFalseOnSalesDataSaveException() {
        SalesDataResponse response = new SalesDataResponse(
                List.of(new SalesDataResponse.SalesTransaction(
                        "MS-TOP-001", LocalDateTime.now(), 1,
                        new BigDecimal("29900"), new BigDecimal("29900"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null)),
                1, "2025-01-01", "2025-01-01");

        when(apiClient.fetchSalesData(any(), any())).thenReturn(response);
        when(salesTransactionRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB error"));

        boolean result = scheduler.collectSalesData(FROM, TO);

        assertThat(result).isFalse();
    }
}
