package com.retaildashboard.service;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockDataGenerator 단위 테스트.
 * 현실적인 한국 리테일 데이터 생성을 검증합니다.
 */
class MockDataGeneratorTest {

    private MockDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MockDataGenerator();
    }

    @Test
    void generateSalesData_shouldReturnTransactionsForDateRange() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 7);

        SalesDataResponse response = generator.generateSalesData(from, to);

        assertNotNull(response);
        assertFalse(response.transactions().isEmpty());
        assertEquals(response.transactions().size(), response.totalCount());
        assertEquals("2024-06-01", response.fromDate());
        assertEquals("2024-06-07", response.toDate());
    }

    @Test
    void generateSalesData_transactionsShouldHaveValidFields() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 1);

        SalesDataResponse response = generator.generateSalesData(from, to);

        for (SalesDataResponse.SalesTransaction tx : response.transactions()) {
            assertNotNull(tx.sku());
            assertFalse(tx.sku().isBlank());
            assertNotNull(tx.transactionTime());
            assertTrue(tx.quantity() > 0);
            assertTrue(tx.unitPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(tx.totalAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(tx.discountAmount().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(tx.returnAmount().compareTo(BigDecimal.ZERO) >= 0);
            assertNotNull(tx.channel());
        }
    }

    @Test
    void generateAdvertisingCosts_shouldReturnCostsForAllChannels() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 3);

        AdvertisingCostResponse response = generator.generateAdvertisingCosts(from, to);

        assertNotNull(response);
        assertFalse(response.costs().isEmpty());
        // 3 days * 4 channels = 12 entries
        assertEquals(12, response.totalCount());

        for (AdvertisingCostResponse.AdvertisingCost cost : response.costs()) {
            assertNotNull(cost.channel());
            assertTrue(cost.spendAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(cost.impressions() > 0);
            assertTrue(cost.clicks() >= 0);
            assertTrue(cost.newCustomers() >= 1);
        }
    }

    @Test
    void generateInventory_shouldReturnItemsForAllSkus() {
        InventoryResponse response = generator.generateInventory();

        assertNotNull(response);
        assertFalse(response.items().isEmpty());
        assertEquals(response.items().size(), response.totalCount());

        for (InventoryResponse.InventoryItem item : response.items()) {
            assertNotNull(item.sku());
            assertTrue(item.currentQuantity() >= 0);
            assertTrue(item.reorderPoint() > 0);
            assertTrue(item.unitCost().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(item.valuationMethod());
            assertTrue(item.valuationMethod().equals("FIFO")
                    || item.valuationMethod().equals("LIFO")
                    || item.valuationMethod().equals("WEIGHTED_AVG"));
        }
    }

    @Test
    void generatePnlMetrics_shouldReturnValidMetrics() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);

        PnlMetricsResponse response = generator.generatePnlMetrics(from, to);

        assertNotNull(response);
        assertTrue(response.totalRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.netRevenue().compareTo(response.totalRevenue()) <= 0);
        assertTrue(response.cogs().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.grossProfit().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.ebitda().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.operatingProfit().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.netProfit().compareTo(BigDecimal.ZERO) > 0);

        // Margin percentages should be between 0 and 100
        assertTrue(response.grossMarginPercent().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.grossMarginPercent().compareTo(new BigDecimal("100")) < 0);
    }

    @Test
    void generatePnlMetrics_shouldIncludeAllAdChannels() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);

        PnlMetricsResponse response = generator.generatePnlMetrics(from, to);

        assertNotNull(response.advertisingMetrics());
        assertTrue(response.advertisingMetrics().containsKey("NAVER"));
        assertTrue(response.advertisingMetrics().containsKey("GOOGLE"));
        assertTrue(response.advertisingMetrics().containsKey("META"));
        assertTrue(response.advertisingMetrics().containsKey("OTHERS"));

        for (PnlMetricsResponse.ChannelAdMetrics metrics : response.advertisingMetrics().values()) {
            assertTrue(metrics.spend().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(metrics.roas().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void generatePnlMetrics_shouldIncludeCategoryBreakdown() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);

        PnlMetricsResponse response = generator.generatePnlMetrics(from, to);

        assertNotNull(response.categoryBreakdown());
        assertFalse(response.categoryBreakdown().isEmpty());

        for (PnlMetricsResponse.CategoryBreakdown cat : response.categoryBreakdown()) {
            assertNotNull(cat.category());
            assertTrue(cat.revenue().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(cat.salesVolume() > 0);
        }
    }
}
