package com.retaildashboard.service;

import com.retaildashboard.dto.mock.AdvertisingCostResponse;
import com.retaildashboard.dto.mock.InventoryResponse;
import com.retaildashboard.dto.mock.PnlMetricsResponse;
import com.retaildashboard.dto.mock.SalesDataResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 현실적인 한국 리테일 테스트 데이터를 생성하는 서비스.
 *
 * 상품 계층: 의류 > 상의/하의/아우터 > 브랜드 > SKU
 * 광고 채널: Naver, Google, Meta, Others
 * 계절별 판매 패턴 반영
 *
 * Requirements: 1.1, 1.2
 */
@Service
public class MockDataGenerator {

    private final Random random = new Random(42);

    // === 상품 계층 시드 데이터 ===

    private static final Map<String, List<String>> SUBCATEGORIES = Map.of(
            "의류", List.of("상의", "하의", "아우터")
    );

    private static final Map<String, List<String>> BRANDS_BY_SUBCATEGORY = Map.of(
            "상의", List.of("무신사 스탠다드", "커버낫", "디스이즈네버댓"),
            "하의", List.of("무신사 스탠다드", "스파오", "유니클로"),
            "아우터", List.of("내셔널지오그래픽", "노스페이스", "디스커버리")
    );

    private static final Map<String, List<SkuInfo>> SKUS_BY_BRAND;

    static {
        Map<String, List<SkuInfo>> skus = new HashMap<>();
        // 상의 브랜드
        skus.put("무신사 스탠다드_상의", List.of(
                new SkuInfo("MS-TOP-001", "에센셜 반팔 티셔츠", "화이트", "M", new BigDecimal("15000"), new BigDecimal("29900")),
                new SkuInfo("MS-TOP-002", "에센셜 반팔 티셔츠", "블랙", "L", new BigDecimal("15000"), new BigDecimal("29900")),
                new SkuInfo("MS-TOP-003", "오버핏 맨투맨", "그레이", "L", new BigDecimal("22000"), new BigDecimal("39900"))
        ));
        skus.put("커버낫_상의", List.of(
                new SkuInfo("CV-TOP-001", "C 로고 티셔츠", "네이비", "M", new BigDecimal("18000"), new BigDecimal("35900")),
                new SkuInfo("CV-TOP-002", "스트라이프 셔츠", "블루", "L", new BigDecimal("25000"), new BigDecimal("49900"))
        ));
        skus.put("디스이즈네버댓_상의", List.of(
                new SkuInfo("TN-TOP-001", "T-로고 후드", "블랙", "M", new BigDecimal("35000"), new BigDecimal("69900")),
                new SkuInfo("TN-TOP-002", "아치 로고 맨투맨", "크림", "L", new BigDecimal("30000"), new BigDecimal("59900"))
        ));
        // 하의 브랜드
        skus.put("무신사 스탠다드_하의", List.of(
                new SkuInfo("MS-BOT-001", "와이드 데님 팬츠", "인디고", "M", new BigDecimal("25000"), new BigDecimal("49900")),
                new SkuInfo("MS-BOT-002", "슬랙스", "블랙", "L", new BigDecimal("20000"), new BigDecimal("39900"))
        ));
        skus.put("스파오_하의", List.of(
                new SkuInfo("SP-BOT-001", "쿨 조거팬츠", "차콜", "M", new BigDecimal("12000"), new BigDecimal("25900")),
                new SkuInfo("SP-BOT-002", "에어리 숏팬츠", "카키", "L", new BigDecimal("10000"), new BigDecimal("19900"))
        ));
        skus.put("유니클로_하의", List.of(
                new SkuInfo("UQ-BOT-001", "에어리즘 이지팬츠", "네이비", "M", new BigDecimal("15000"), new BigDecimal("29900")),
                new SkuInfo("UQ-BOT-002", "스마트 앵클팬츠", "그레이", "L", new BigDecimal("18000"), new BigDecimal("39900"))
        ));
        // 아우터 브랜드
        skus.put("내셔널지오그래픽_아우터", List.of(
                new SkuInfo("NG-OUT-001", "칼리 플리스 자켓", "아이보리", "M", new BigDecimal("55000"), new BigDecimal("129000")),
                new SkuInfo("NG-OUT-002", "에코 패딩 점퍼", "블랙", "L", new BigDecimal("80000"), new BigDecimal("189000"))
        ));
        skus.put("노스페이스_아우터", List.of(
                new SkuInfo("NF-OUT-001", "눕시 다운 자켓", "블랙", "M", new BigDecimal("120000"), new BigDecimal("289000")),
                new SkuInfo("NF-OUT-002", "화이트라벨 플리스", "베이지", "L", new BigDecimal("45000"), new BigDecimal("99000"))
        ));
        skus.put("디스커버리_아우터", List.of(
                new SkuInfo("DC-OUT-001", "레스터 롱패딩", "카키", "M", new BigDecimal("90000"), new BigDecimal("199000")),
                new SkuInfo("DC-OUT-002", "버킷 플리스", "브라운", "L", new BigDecimal("50000"), new BigDecimal("119000"))
        ));
        SKUS_BY_BRAND = Collections.unmodifiableMap(skus);
    }

    private static final String[] AD_CHANNELS = {"NAVER", "GOOGLE", "META", "OTHERS"};
    private static final String[] SALES_CHANNELS = {"온라인몰", "네이버스토어", "쿠팡", "무신사", "오프라인"};

    /**
     * 판매 데이터 생성.
     * 계절별 패턴을 반영하여 현실적인 거래 데이터를 생성합니다.
     */
    public SalesDataResponse generateSalesData(LocalDate from, LocalDate to) {
        List<SalesDataResponse.SalesTransaction> transactions = new ArrayList<>();
        List<SkuInfo> allSkus = getAllSkus();

        LocalDate current = from;
        while (!current.isAfter(to)) {
            double seasonalMultiplier = getSeasonalMultiplier(current);
            int dailyTransactionCount = (int) (randomBetween(30, 80) * seasonalMultiplier);

            for (int i = 0; i < dailyTransactionCount; i++) {
                SkuInfo sku = allSkus.get(random.nextInt(allSkus.size()));
                int hour = randomBetween(9, 23);
                int minute = random.nextInt(60);
                LocalDateTime txTime = LocalDateTime.of(current, LocalTime.of(hour, minute));

                int quantity = randomBetween(1, 5);
                BigDecimal unitPrice = sku.retailPrice();
                BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

                // 10% 확률로 할인 적용
                BigDecimal discountAmount = BigDecimal.ZERO;
                if (random.nextDouble() < 0.10) {
                    double discountRate = 0.05 + random.nextDouble() * 0.25;
                    discountAmount = totalAmount.multiply(BigDecimal.valueOf(discountRate))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                // 3% 확률로 반품
                BigDecimal returnAmount = BigDecimal.ZERO;
                if (random.nextDouble() < 0.03) {
                    returnAmount = totalAmount;
                }

                // 5% 확률로 번들 상품
                boolean isBundle = random.nextDouble() < 0.05;
                Integer bundleId = isBundle ? randomBetween(1000, 9999) : null;

                String channel = SALES_CHANNELS[random.nextInt(SALES_CHANNELS.length)];

                transactions.add(new SalesDataResponse.SalesTransaction(
                        sku.sku(), txTime, quantity, unitPrice, totalAmount,
                        discountAmount, returnAmount, channel, isBundle, bundleId
                ));
            }
            current = current.plusDays(1);
        }

        return new SalesDataResponse(
                transactions,
                transactions.size(),
                from.toString(),
                to.toString()
        );
    }

    /**
     * 광고비 데이터 생성.
     * 채널별(Naver, Google, Meta, Others) 현실적인 광고 지출 데이터를 생성합니다.
     */
    public AdvertisingCostResponse generateAdvertisingCosts(LocalDate from, LocalDate to) {
        List<AdvertisingCostResponse.AdvertisingCost> costs = new ArrayList<>();

        LocalDate current = from;
        while (!current.isAfter(to)) {
            double seasonalMultiplier = getSeasonalMultiplier(current);

            for (String channel : AD_CHANNELS) {
                BigDecimal baseSpend = getBaseAdSpend(channel);
                BigDecimal spend = baseSpend
                        .multiply(BigDecimal.valueOf(seasonalMultiplier))
                        .multiply(BigDecimal.valueOf(0.8 + random.nextDouble() * 0.4))
                        .setScale(2, RoundingMode.HALF_UP);

                int impressions = (int) (spend.doubleValue() * randomBetween(80, 150));
                double ctr = 0.01 + random.nextDouble() * 0.04;
                int clicks = (int) (impressions * ctr);
                double conversionRate = 0.02 + random.nextDouble() * 0.06;
                int conversions = (int) (clicks * conversionRate);
                int newCustomers = (int) (conversions * (0.3 + random.nextDouble() * 0.4));

                costs.add(new AdvertisingCostResponse.AdvertisingCost(
                        channel, current, spend, impressions, clicks, conversions,
                        Math.max(newCustomers, 1)
                ));
            }
            current = current.plusDays(1);
        }

        return new AdvertisingCostResponse(
                costs,
                costs.size(),
                from.toString(),
                to.toString()
        );
    }

    /**
     * 재고 데이터 생성.
     * 모든 SKU에 대한 현재 재고 상태를 생성합니다.
     */
    public InventoryResponse generateInventory() {
        List<InventoryResponse.InventoryItem> items = new ArrayList<>();
        List<SkuInfo> allSkus = getAllSkus();

        for (SkuInfo sku : allSkus) {
            int currentQuantity = randomBetween(10, 500);
            int reorderPoint = randomBetween(20, 100);
            String[] methods = {"FIFO", "LIFO", "WEIGHTED_AVG"};
            String valuationMethod = methods[random.nextInt(methods.length)];

            items.add(new InventoryResponse.InventoryItem(
                    sku.sku(),
                    currentQuantity,
                    reorderPoint,
                    sku.unitCost(),
                    valuationMethod,
                    LocalDateTime.now().minusHours(randomBetween(1, 24))
            ));
        }

        return new InventoryResponse(items, items.size());
    }

    /**
     * P&L 메트릭 생성.
     * 매출, 비용, 이익 등 종합 P&L 지표를 생성합니다.
     */
    public PnlMetricsResponse generatePnlMetrics(LocalDate from, LocalDate to) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        double seasonalAvg = getAverageSeasonalMultiplier(from, to);

        // 매출 계산
        BigDecimal dailyBaseRevenue = new BigDecimal("5000000"); // 일 500만원 기준
        BigDecimal totalRevenue = dailyBaseRevenue
                .multiply(BigDecimal.valueOf(daysBetween))
                .multiply(BigDecimal.valueOf(seasonalAvg))
                .multiply(BigDecimal.valueOf(0.9 + random.nextDouble() * 0.2))
                .setScale(0, RoundingMode.HALF_UP);

        // 반품/할인 (약 8%)
        BigDecimal returnsAndDiscounts = totalRevenue
                .multiply(new BigDecimal("0.08"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal netRevenue = totalRevenue.subtract(returnsAndDiscounts);

        // COGS (매출원가, 약 45%)
        BigDecimal cogs = netRevenue
                .multiply(new BigDecimal("0.45"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal grossProfit = netRevenue.subtract(cogs);

        // 영업비용 (매출총이익의 약 30%)
        BigDecimal operatingExpenses = grossProfit
                .multiply(new BigDecimal("0.30"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal ebitda = grossProfit.subtract(operatingExpenses);

        // 감가상각비 (EBITDA의 약 5%)
        BigDecimal depreciation = ebitda
                .multiply(new BigDecimal("0.05"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal operatingProfit = ebitda.subtract(depreciation);

        // 이자 + 세금 (영업이익의 약 25%)
        BigDecimal interestAndTax = operatingProfit
                .multiply(new BigDecimal("0.25"))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal netProfit = operatingProfit.subtract(interestAndTax);

        // 마진율 계산
        BigDecimal grossMargin = grossProfit.multiply(new BigDecimal("100"))
                .divide(netRevenue, 2, RoundingMode.HALF_UP);
        BigDecimal operatingMargin = operatingProfit.multiply(new BigDecimal("100"))
                .divide(netRevenue, 2, RoundingMode.HALF_UP);
        BigDecimal netMargin = netProfit.multiply(new BigDecimal("100"))
                .divide(netRevenue, 2, RoundingMode.HALF_UP);

        // 채널별 광고 메트릭
        Map<String, PnlMetricsResponse.ChannelAdMetrics> adMetrics = new LinkedHashMap<>();
        for (String channel : AD_CHANNELS) {
            BigDecimal spend = getBaseAdSpend(channel)
                    .multiply(BigDecimal.valueOf(daysBetween))
                    .multiply(BigDecimal.valueOf(seasonalAvg))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal channelRevenue = spend
                    .multiply(BigDecimal.valueOf(2.5 + random.nextDouble() * 3.0))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal roas = channelRevenue.divide(spend, 2, RoundingMode.HALF_UP);
            int conversions = (int) (spend.doubleValue() / 5000 * (0.8 + random.nextDouble() * 0.4));
            int newCustomers = (int) (conversions * (0.3 + random.nextDouble() * 0.4));
            BigDecimal cac = newCustomers > 0
                    ? spend.divide(BigDecimal.valueOf(newCustomers), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            adMetrics.put(channel, new PnlMetricsResponse.ChannelAdMetrics(
                    spend, channelRevenue, roas, conversions,
                    Math.max(newCustomers, 1), cac
            ));
        }

        // 카테고리별 매출 분해
        List<PnlMetricsResponse.CategoryBreakdown> categoryBreakdown = new ArrayList<>();
        Map<String, Double> categoryShares = Map.of("상의", 0.40, "하의", 0.30, "아우터", 0.30);
        for (var entry : categoryShares.entrySet()) {
            BigDecimal catRevenue = totalRevenue.multiply(BigDecimal.valueOf(entry.getValue()))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal catCogs = catRevenue.multiply(new BigDecimal("0.45"))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal catGrossProfit = catRevenue.subtract(catCogs);
            int salesVolume = (int) (catRevenue.doubleValue() / 50000);

            categoryBreakdown.add(new PnlMetricsResponse.CategoryBreakdown(
                    entry.getKey(), catRevenue, catCogs, catGrossProfit, salesVolume
            ));
        }

        return new PnlMetricsResponse(
                totalRevenue, netRevenue, cogs, grossProfit,
                ebitda, operatingProfit, netProfit,
                grossMargin, operatingMargin, netMargin,
                adMetrics, categoryBreakdown,
                from.toString(), to.toString()
        );
    }

    // === Private Helper Methods ===

    private List<SkuInfo> getAllSkus() {
        List<SkuInfo> allSkus = new ArrayList<>();
        SKUS_BY_BRAND.values().forEach(allSkus::addAll);
        return allSkus;
    }

    /**
     * 계절별 판매 배수를 반환합니다.
     * 봄/가을 시즌(3-4월, 9-10월)과 겨울 시즌(11-12월)에 판매량 증가.
     */
    private double getSeasonalMultiplier(LocalDate date) {
        int month = date.getMonthValue();
        return switch (month) {
            case 1 -> 0.7;   // 비수기
            case 2 -> 0.8;   // 비수기
            case 3 -> 1.1;   // 봄 시즌 시작
            case 4 -> 1.2;   // 봄 시즌 피크
            case 5 -> 1.0;   // 보통
            case 6 -> 0.9;   // 여름 비수기
            case 7 -> 0.85;  // 여름 비수기
            case 8 -> 0.9;   // 여름 비수기
            case 9 -> 1.15;  // 가을 시즌 시작
            case 10 -> 1.3;  // 가을 시즌 피크
            case 11 -> 1.5;  // 겨울 시즌 (블랙프라이데이)
            case 12 -> 1.4;  // 겨울 시즌 (연말)
            default -> 1.0;
        };
    }

    private double getAverageSeasonalMultiplier(LocalDate from, LocalDate to) {
        double sum = 0;
        int count = 0;
        LocalDate current = from;
        while (!current.isAfter(to)) {
            sum += getSeasonalMultiplier(current);
            count++;
            current = current.plusDays(1);
        }
        return count > 0 ? sum / count : 1.0;
    }

    /**
     * 채널별 일일 기본 광고비를 반환합니다.
     */
    private BigDecimal getBaseAdSpend(String channel) {
        return switch (channel) {
            case "NAVER" -> new BigDecimal("350000");   // 네이버 일 35만원
            case "GOOGLE" -> new BigDecimal("250000");  // 구글 일 25만원
            case "META" -> new BigDecimal("200000");    // 메타 일 20만원
            case "OTHERS" -> new BigDecimal("100000");  // 기타 일 10만원
            default -> new BigDecimal("100000");
        };
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private record SkuInfo(
            String sku,
            String name,
            String color,
            String size,
            BigDecimal unitCost,
            BigDecimal retailPrice
    ) {}
}
