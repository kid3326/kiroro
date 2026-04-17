package com.retaildashboard.controller.mock;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import com.retaildashboard.service.MockDataGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockApiController 단위 테스트.
 * 토큰 인증, 엔드포인트 응답, 날짜 파라미터 처리를 검증합니다.
 */
@WebMvcTest(MockApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class MockApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MockDataGenerator mockDataGenerator;

    private static final String VALID_TOKEN = "Bearer mock-api-secret-token-2024";

    // === Authentication Tests ===

    @Test
    void salesEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authorization header is required"));
    }

    @Test
    void salesEndpoint_withInvalidScheme_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/sales")
                        .header("Authorization", "Basic invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authorization header must use Bearer scheme"));
    }

    @Test
    void salesEndpoint_withInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/sales")
                        .header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API token"));
    }

    // === Sales Endpoint Tests ===

    @Test
    void salesEndpoint_withValidToken_shouldReturn200() throws Exception {
        SalesDataResponse mockResponse = new SalesDataResponse(
                List.of(new SalesDataResponse.SalesTransaction(
                        "MS-TOP-001", LocalDateTime.of(2024, 6, 1, 10, 0),
                        2, new BigDecimal("29900"), new BigDecimal("59800"),
                        BigDecimal.ZERO, BigDecimal.ZERO, "온라인몰", false, null
                )),
                1, "2024-06-01", "2024-06-01"
        );

        org.mockito.Mockito.when(mockDataGenerator.generateSalesData(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/mock/api/sales")
                        .header("Authorization", VALID_TOKEN)
                        .param("from", "2024-06-01")
                        .param("to", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.transactions[0].sku").value("MS-TOP-001"));
    }

    @Test
    void salesEndpoint_withInvalidDateFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/mock/api/sales")
                        .header("Authorization", VALID_TOKEN)
                        .param("from", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // === Costs Endpoint Tests ===

    @Test
    void costsEndpoint_withValidToken_shouldReturn200() throws Exception {
        AdvertisingCostResponse mockResponse = new AdvertisingCostResponse(
                List.of(new AdvertisingCostResponse.AdvertisingCost(
                        "NAVER", LocalDate.of(2024, 6, 1),
                        new BigDecimal("350000"), 50000, 1500, 45, 15
                )),
                1, "2024-06-01", "2024-06-01"
        );

        org.mockito.Mockito.when(mockDataGenerator.generateAdvertisingCosts(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/mock/api/costs")
                        .header("Authorization", VALID_TOKEN)
                        .param("from", "2024-06-01")
                        .param("to", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.costs[0].channel").value("NAVER"));
    }

    @Test
    void costsEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/costs"))
                .andExpect(status().isUnauthorized());
    }

    // === Inventory Endpoint Tests ===

    @Test
    void inventoryEndpoint_withValidToken_shouldReturn200() throws Exception {
        InventoryResponse mockResponse = new InventoryResponse(
                List.of(new InventoryResponse.InventoryItem(
                        "MS-TOP-001", 150, 50,
                        new BigDecimal("15000"), "WEIGHTED_AVG",
                        LocalDateTime.now()
                )),
                1
        );

        org.mockito.Mockito.when(mockDataGenerator.generateInventory())
                .thenReturn(mockResponse);

        mockMvc.perform(get("/mock/api/inventory")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].sku").value("MS-TOP-001"));
    }

    @Test
    void inventoryEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/inventory"))
                .andExpect(status().isUnauthorized());
    }

    // === PnL Endpoint Tests ===

    @Test
    void pnlEndpoint_withValidToken_shouldReturn200() throws Exception {
        PnlMetricsResponse mockResponse = new PnlMetricsResponse(
                new BigDecimal("150000000"), new BigDecimal("138000000"),
                new BigDecimal("62100000"), new BigDecimal("75900000"),
                new BigDecimal("53130000"), new BigDecimal("50473500"),
                new BigDecimal("37855125"),
                new BigDecimal("55.00"), new BigDecimal("36.57"),
                new BigDecimal("27.43"),
                Map.of("NAVER", new PnlMetricsResponse.ChannelAdMetrics(
                        new BigDecimal("10500000"), new BigDecimal("35000000"),
                        new BigDecimal("3.33"), 700, 210, new BigDecimal("50000")
                )),
                List.of(new PnlMetricsResponse.CategoryBreakdown(
                        "상의", new BigDecimal("60000000"), new BigDecimal("27000000"),
                        new BigDecimal("33000000"), 1200
                )),
                "2024-06-01", "2024-06-30"
        );

        org.mockito.Mockito.when(mockDataGenerator.generatePnlMetrics(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/mock/api/pnl")
                        .header("Authorization", VALID_TOKEN)
                        .param("from", "2024-06-01")
                        .param("to", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(150000000))
                .andExpect(jsonPath("$.advertisingMetrics.NAVER.roas").value(3.33));
    }

    @Test
    void pnlEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/mock/api/pnl"))
                .andExpect(status().isUnauthorized());
    }
}
