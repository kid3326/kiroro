package com.retaildashboard.controller;

import com.retaildashboard.config.Audited;
import com.retaildashboard.domain.AdChannel;
import com.retaildashboard.domain.DailyAggregate;
import com.retaildashboard.domain.DataType;
import com.retaildashboard.domain.HierarchyLevel;
import com.retaildashboard.domain.Inventory;
import com.retaildashboard.domain.MetricType;
import com.retaildashboard.domain.User;
import com.retaildashboard.dto.ComparisonResult;
import com.retaildashboard.dto.DashboardSummaryResponse;
import com.retaildashboard.dto.DateRange;
import com.retaildashboard.dto.FilterCriteria;
import com.retaildashboard.dto.ProductMetrics;
import com.retaildashboard.dto.TimeSeriesDataPoint;
import com.retaildashboard.repository.DailyAggregateRepository;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.service.AdvertisingMetricsService;
import com.retaildashboard.service.AuthorizationService;
import com.retaildashboard.service.DataAggregationService;
import com.retaildashboard.service.ProductAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 대시보드 REST API 컨트롤러.
 * P&L 요약, 매출, 비용, 광고비, 상품별, 재고, 비교 데이터를 제공합니다.
 *
 * 공통 쿼리 파라미터: from, to, category, brand, sku, channel, granularity, page, size
 * 페이지네이션 기본 size=50
 *
 * Requirements: 6.1, 7.3, 8.3
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DataAggregationService dataAggregationService;
    private final AdvertisingMetricsService advertisingMetricsService;
    private final ProductAggregationService productAggregationService;
    private final AuthorizationService authorizationService;
    private final DailyAggregateRepository dailyAggregateRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * P&L 요약 메트릭을 조회합니다.
     * GET /api/v1/dashboard/summary
     *
     * @param from        시작 날짜 (ISO 8601)
     * @param to          종료 날짜 (ISO 8601)
     * @param category    카테고리 필터
     * @param brand       브랜드 필터
     * @param sku         SKU 필터
     * @param channel     채널 필터
     * @param authentication 인증 정보
     * @return P&L 요약 응답
     */
    @GetMapping("/summary")
    @Audited(eventType = "DATA_ACCESS", dataType = "SALES", dataScope = "dashboard_summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String channel,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        DateRange range = new DateRange(from, to);
        FilterCriteria filters = buildFilterCriteria(category, brand, sku, channel);

        BigDecimal totalRevenue = dataAggregationService.calculateTotalRevenue(range, filters);
        BigDecimal netRevenue = dataAggregationService.calculateNetRevenue(range, filters);
        BigDecimal grossProfit = dataAggregationService.calculateGrossProfit(range, filters);
        BigDecimal ebitda = dataAggregationService.calculateEBITDA(range, filters);
        BigDecimal operatingProfit = dataAggregationService.calculateOperatingProfit(range, filters);
        BigDecimal netProfit = dataAggregationService.calculateNetProfit(range, filters);
        BigDecimal totalAdSpend = advertisingMetricsService.calculateTotalAdSpend(range);

        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .netRevenue(netRevenue)
                .grossProfit(grossProfit)
                .ebitda(ebitda)
                .operatingProfit(operatingProfit)
                .netProfit(netProfit)
                .totalAdSpend(totalAdSpend)
                .fromDate(from.toString())
                .toDate(to.toString())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 매출 시계열 데이터를 조회합니다.
     * GET /api/v1/dashboard/revenue
     *
     * @param from        시작 날짜
     * @param to          종료 날짜
     * @param category    카테고리 필터
     * @param brand       브랜드 필터
     * @param sku         SKU 필터
     * @param granularity 시간 단위 (daily, weekly, monthly, quarterly, yearly)
     * @param page        페이지 번호
     * @param size        페이지 크기 (기본 50)
     * @param authentication 인증 정보
     * @return 시계열 데이터 페이지
     */
    @GetMapping("/revenue")
    @Audited(eventType = "DATA_ACCESS", dataType = "SALES", dataScope = "revenue_timeseries")
    public ResponseEntity<Page<TimeSeriesDataPoint>> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false, defaultValue = "daily") String granularity,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        List<DailyAggregate> aggregates = fetchDailyAggregates(from, to, category, brand, sku);

        List<TimeSeriesDataPoint> dataPoints = aggregates.stream()
                .map(this::toTimeSeriesDataPoint)
                .collect(Collectors.toList());

        Page<TimeSeriesDataPoint> pagedResult = paginateList(dataPoints, page, size);
        return ResponseEntity.ok(pagedResult);
    }

    /**
     * 비용 데이터를 조회합니다. 권한 체크를 포함합니다.
     * GET /api/v1/dashboard/costs
     *
     * Finance/Executive 이상 역할만 접근 가능합니다.
     *
     * @param from        시작 날짜
     * @param to          종료 날짜
     * @param category    카테고리 필터
     * @param brand       브랜드 필터
     * @param sku         SKU 필터
     * @param granularity 시간 단위
     * @param page        페이지 번호
     * @param size        페이지 크기 (기본 50)
     * @param authentication 인증 정보
     * @return 비용 시계열 데이터 페이지
     */
    @GetMapping("/costs")
    @Audited(eventType = "DATA_ACCESS", dataType = "COSTS", dataScope = "costs_data")
    public ResponseEntity<Page<TimeSeriesDataPoint>> getCosts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false, defaultValue = "daily") String granularity,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        authorizationService.checkAccess(user, DataType.COSTS);

        List<DailyAggregate> aggregates = fetchDailyAggregates(from, to, category, brand, sku);

        List<TimeSeriesDataPoint> dataPoints = aggregates.stream()
                .map(this::toTimeSeriesDataPoint)
                .collect(Collectors.toList());

        Page<TimeSeriesDataPoint> pagedResult = paginateList(dataPoints, page, size);
        return ResponseEntity.ok(pagedResult);
    }

    /**
     * 광고비 데이터를 조회합니다. 권한 체크를 포함합니다.
     * GET /api/v1/dashboard/advertising
     *
     * Marketing 이상 역할만 접근 가능합니다.
     *
     * @param from    시작 날짜
     * @param to      종료 날짜
     * @param channel 광고 채널 필터
     * @param page    페이지 번호
     * @param size    페이지 크기 (기본 50)
     * @param authentication 인증 정보
     * @return 광고비 데이터
     */
    @GetMapping("/advertising")
    @Audited(eventType = "DATA_ACCESS", dataType = "ADVERTISING", dataScope = "advertising_data")
    public ResponseEntity<Map<String, Object>> getAdvertising(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        authorizationService.checkAccess(user, DataType.ADVERTISING);

        DateRange range = new DateRange(from, to);

        BigDecimal totalAdSpend = advertisingMetricsService.calculateTotalAdSpend(range);
        BigDecimal cac = advertisingMetricsService.calculateCAC(range);

        Map<String, BigDecimal> channelSpend = new java.util.LinkedHashMap<>();
        Map<String, BigDecimal> channelRoas = new java.util.LinkedHashMap<>();

        for (AdChannel adChannel : AdChannel.values()) {
            if (channel == null || channel.equalsIgnoreCase(adChannel.name())) {
                BigDecimal spend = advertisingMetricsService.calculateAdSpendByChannel(range, adChannel);
                BigDecimal roas = advertisingMetricsService.calculateROAS(range, adChannel);
                channelSpend.put(adChannel.name(), spend);
                channelRoas.put(adChannel.name(), roas);
            }
        }

        Map<String, Object> response = Map.of(
                "totalAdSpend", totalAdSpend,
                "cac", cac,
                "channelSpend", channelSpend,
                "channelRoas", channelRoas,
                "fromDate", from.toString(),
                "toDate", to.toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 상품별 데이터를 조회합니다 (계층 지원).
     * GET /api/v1/dashboard/products
     *
     * @param from        시작 날짜
     * @param to          종료 날짜
     * @param level       계층 레벨 (CATEGORY, SUBCATEGORY, BRAND, SKU)
     * @param page        페이지 번호
     * @param size        페이지 크기 (기본 50)
     * @param authentication 인증 정보
     * @return 상품별 메트릭 페이지
     */
    @GetMapping("/products")
    @Audited(eventType = "DATA_ACCESS", dataType = "PRODUCT", dataScope = "products_data")
    public ResponseEntity<Page<ProductMetrics>> getProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "CATEGORY") String level,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        authorizationService.checkAccess(user, DataType.PRODUCT);

        DateRange range = new DateRange(from, to);
        HierarchyLevel hierarchyLevel = HierarchyLevel.valueOf(level.toUpperCase());

        List<ProductMetrics> metrics = productAggregationService.aggregateByHierarchy(hierarchyLevel, range);

        Page<ProductMetrics> pagedResult = paginateList(metrics, page, size);
        return ResponseEntity.ok(pagedResult);
    }

    /**
     * 재고 데이터를 조회합니다.
     * GET /api/v1/dashboard/inventory
     *
     * @param sku  SKU 필터
     * @param page 페이지 번호
     * @param size 페이지 크기 (기본 50)
     * @param authentication 인증 정보
     * @return 재고 데이터 페이지
     */
    @GetMapping("/inventory")
    @Audited(eventType = "DATA_ACCESS", dataType = "INVENTORY", dataScope = "inventory_data")
    public ResponseEntity<Page<Inventory>> getInventory(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        authorizationService.checkAccess(user, DataType.INVENTORY);

        PageRequest pageable = PageRequest.of(page, size);

        Page<Inventory> inventoryPage;
        if (sku != null && !sku.isBlank()) {
            List<Inventory> inventoryList = inventoryRepository.findBySku(sku)
                    .map(List::of)
                    .orElse(List.of());
            inventoryPage = new PageImpl<>(inventoryList, pageable, inventoryList.size());
        } else {
            inventoryPage = inventoryRepository.findAll(pageable);
        }

        return ResponseEntity.ok(inventoryPage);
    }

    /**
     * YoY/MoM 비교 데이터를 조회합니다.
     * GET /api/v1/dashboard/comparison
     *
     * @param from   시작 날짜
     * @param to     종료 날짜
     * @param metric 비교 메트릭 (TOTAL_REVENUE, NET_REVENUE, GROSS_PROFIT, EBITDA, OPERATING_PROFIT, NET_PROFIT, AD_SPEND)
     * @param type   비교 유형 (yoy, mom, budget)
     * @param authentication 인증 정보
     * @return 비교 결과
     */
    @GetMapping("/comparison")
    @Audited(eventType = "DATA_ACCESS", dataType = "SALES", dataScope = "comparison_data")
    public ResponseEntity<ComparisonResult> getComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "TOTAL_REVENUE") String metric,
            @RequestParam(required = false, defaultValue = "yoy") String type,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        DateRange range = new DateRange(from, to);
        MetricType metricType = MetricType.valueOf(metric.toUpperCase());

        ComparisonResult result = switch (type.toLowerCase()) {
            case "mom" -> advertisingMetricsService.calculateMoM(range, metricType);
            case "budget" -> advertisingMetricsService.calculateBudgetVariance(range, metricType);
            default -> advertisingMetricsService.calculateYoY(range, metricType);
        };

        return ResponseEntity.ok(result);
    }

    // ---- Helper methods ----

    private FilterCriteria buildFilterCriteria(String category, String brand, String sku, String channel) {
        return FilterCriteria.builder()
                .category(category)
                .brand(brand)
                .sku(sku)
                .channel(channel)
                .build();
    }

    private List<DailyAggregate> fetchDailyAggregates(LocalDate from, LocalDate to,
                                                       String category, String brand, String sku) {
        if (sku != null && !sku.isBlank()) {
            return dailyAggregateRepository.findByAggregateDateBetweenAndSku(from, to, sku);
        }
        if (brand != null && !brand.isBlank()) {
            return dailyAggregateRepository.findByAggregateDateBetweenAndBrand(from, to, brand);
        }
        if (category != null && !category.isBlank()) {
            return dailyAggregateRepository.findByAggregateDateBetweenAndCategory(from, to, category);
        }
        return dailyAggregateRepository.findByAggregateDateBetween(from, to);
    }

    private TimeSeriesDataPoint toTimeSeriesDataPoint(DailyAggregate agg) {
        return TimeSeriesDataPoint.builder()
                .date(agg.getAggregateDate())
                .totalRevenue(agg.getTotalRevenue())
                .netRevenue(agg.getNetRevenue())
                .cogs(agg.getCogs())
                .grossProfit(agg.getGrossProfit())
                .salesVolume(agg.getSalesVolume())
                .adSpend(agg.getAdSpend())
                .roas(agg.getRoas())
                .build();
    }

    private <T> Page<T> paginateList(List<T> list, int page, int size) {
        int start = Math.min(page * size, list.size());
        int end = Math.min(start + size, list.size());
        List<T> subList = list.subList(start, end);
        return new PageImpl<>(subList, PageRequest.of(page, size), list.size());
    }
}
