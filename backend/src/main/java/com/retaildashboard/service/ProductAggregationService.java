package com.retaildashboard.service;

import com.retaildashboard.domain.HierarchyLevel;
import com.retaildashboard.domain.Inventory;
import com.retaildashboard.domain.InventoryValuationMethod;
import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.dto.DateRange;
import com.retaildashboard.dto.ProductMetrics;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 계층 집계 및 재고 계산 서비스.
 * 4단계 계층(Category → Subcategory → Brand → SKU) 집계,
 * 상품 변형별 판매 추적, 번들 매출 비례 배분, 재고 회전율을 계산합니다.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductAggregationService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final InventoryRepository inventoryRepository;
    private final DataAggregationService dataAggregationService;
    private final EntityManager entityManager;

    /**
     * 지정된 계층 레벨별로 판매량/매출을 집계합니다.
     * Requirement 5.1, 5.2: 4단계 계층별 판매량/매출 집계
     *
     * @param level 계층 레벨
     * @param range 조회 기간
     * @return 계층별 상품 메트릭 목록
     */
    public List<ProductMetrics> aggregateByHierarchy(HierarchyLevel level, DateRange range) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        String jpql = buildHierarchyQuery(level);

        @SuppressWarnings("unchecked")
        List<Tuple> results = entityManager.createQuery(jpql, Tuple.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<ProductMetrics> metrics = new ArrayList<>();
        for (Tuple row : results) {
            metrics.add(ProductMetrics.builder()
                    .hierarchyLevel(level.name())
                    .name(row.get("name", String.class))
                    .salesVolume(row.get("salesVolume", Long.class))
                    .revenue(row.get("revenue", BigDecimal.class))
                    .build());
        }

        return metrics;
    }

    /**
     * 상품 변형(색상/사이즈)별 판매를 추적합니다.
     * Requirement 5.3: 상품 변형별 판매 추적
     *
     * @param range 조회 기간
     * @return 변형별 상품 메트릭 목록
     */
    public List<ProductMetrics> aggregateByVariant(DateRange range) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        String jpql = "SELECT p.sku AS name, p.variantColor AS variantColor, " +
                "p.variantSize AS variantSize, " +
                "COALESCE(SUM(s.quantity), 0L) AS salesVolume, " +
                "COALESCE(SUM(s.totalAmount), 0) AS revenue " +
                "FROM SalesTransaction s " +
                "JOIN Product p ON s.sku = p.sku " +
                "WHERE s.transactionTime BETWEEN :from AND :to " +
                "GROUP BY p.sku, p.variantColor, p.variantSize " +
                "ORDER BY revenue DESC";

        @SuppressWarnings("unchecked")
        List<Tuple> results = entityManager.createQuery(jpql, Tuple.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<ProductMetrics> metrics = new ArrayList<>();
        for (Tuple row : results) {
            metrics.add(ProductMetrics.builder()
                    .hierarchyLevel("VARIANT")
                    .name(row.get("name", String.class))
                    .variantColor(row.get("variantColor", String.class))
                    .variantSize(row.get("variantSize", String.class))
                    .salesVolume(row.get("salesVolume", Long.class))
                    .revenue(row.get("revenue", BigDecimal.class))
                    .build());
        }

        return metrics;
    }

    /**
     * 번들 상품 매출을 비례 배분합니다.
     * Requirement 5.4: 번들 상품 매출을 구성 SKU별 단가 비율로 배분
     *
     * @param range 조회 기간
     * @return SKU별 배분된 매출 맵
     */
    public Map<String, BigDecimal> allocateBundleRevenue(DateRange range) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        List<SalesTransaction> bundleSales = salesTransactionRepository
                .findByTransactionTimeBetweenAndIsBundle(from, to, true);

        Map<String, BigDecimal> allocatedRevenue = new HashMap<>();

        // 번들 ID별로 그룹화
        Map<Integer, List<SalesTransaction>> bundleGroups = new HashMap<>();
        for (SalesTransaction tx : bundleSales) {
            if (tx.getBundleId() != null) {
                bundleGroups.computeIfAbsent(tx.getBundleId(), k -> new ArrayList<>()).add(tx);
            }
        }

        for (Map.Entry<Integer, List<SalesTransaction>> entry : bundleGroups.entrySet()) {
            List<SalesTransaction> bundleItems = entry.getValue();

            // 번들 내 각 SKU의 단가 합계 계산
            BigDecimal totalUnitPrice = bundleItems.stream()
                    .map(SalesTransaction::getUnitPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalUnitPrice.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // 번들 총 매출 (첫 번째 항목의 totalAmount를 번들 매출로 사용)
            BigDecimal bundleTotalAmount = bundleItems.stream()
                    .map(SalesTransaction::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 단가 비율로 매출 배분
            for (SalesTransaction item : bundleItems) {
                BigDecimal ratio = item.getUnitPrice()
                        .divide(totalUnitPrice, 6, RoundingMode.HALF_UP);
                BigDecimal allocated = bundleTotalAmount.multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);

                allocatedRevenue.merge(item.getSku(), allocated, BigDecimal::add);
            }
        }

        return allocatedRevenue;
    }

    /**
     * 재고 회전율을 계산합니다.
     * Requirement 5.6: COGS / 평균 재고 가치
     *
     * @param sku   상품 SKU
     * @param range 조회 기간
     * @return 재고 회전율 (평균 재고 가치가 0이면 ZERO 반환)
     */
    public BigDecimal calculateInventoryTurnover(String sku, DateRange range) {
        // SKU별 COGS 계산 (가중평균법 사용)
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        Long quantitySold = salesTransactionRepository.sumQuantityByPeriodAndSku(from, to, sku);
        if (quantitySold == null || quantitySold == 0) {
            return BigDecimal.ZERO;
        }

        Inventory inventory = inventoryRepository.findBySku(sku).orElse(null);
        if (inventory == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal cogs = inventory.getUnitCost().multiply(BigDecimal.valueOf(quantitySold));

        // 평균 재고 가치 = 현재 재고 수량 × 단가
        BigDecimal avgInventoryValue = inventoryRepository.sumInventoryValueBySku(sku);
        if (avgInventoryValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return cogs.divide(avgInventoryValue, 4, RoundingMode.HALF_UP);
    }

    /**
     * 계층 레벨에 따른 JPQL 쿼리를 생성합니다.
     */
    private String buildHierarchyQuery(HierarchyLevel level) {
        return switch (level) {
            case CATEGORY -> "SELECT pc.name AS name, " +
                    "COALESCE(SUM(CAST(s.quantity AS long)), 0L) AS salesVolume, " +
                    "COALESCE(SUM(s.totalAmount), 0) AS revenue " +
                    "FROM SalesTransaction s " +
                    "JOIN Product p ON s.sku = p.sku " +
                    "JOIN Brand b ON p.brandId = b.id " +
                    "JOIN ProductSubcategory ps ON b.subcategoryId = ps.id " +
                    "JOIN ProductCategory pc ON ps.categoryId = pc.id " +
                    "WHERE s.transactionTime BETWEEN :from AND :to " +
                    "GROUP BY pc.name ORDER BY revenue DESC";

            case SUBCATEGORY -> "SELECT ps.name AS name, " +
                    "COALESCE(SUM(CAST(s.quantity AS long)), 0L) AS salesVolume, " +
                    "COALESCE(SUM(s.totalAmount), 0) AS revenue " +
                    "FROM SalesTransaction s " +
                    "JOIN Product p ON s.sku = p.sku " +
                    "JOIN Brand b ON p.brandId = b.id " +
                    "JOIN ProductSubcategory ps ON b.subcategoryId = ps.id " +
                    "WHERE s.transactionTime BETWEEN :from AND :to " +
                    "GROUP BY ps.name ORDER BY revenue DESC";

            case BRAND -> "SELECT b.name AS name, " +
                    "COALESCE(SUM(CAST(s.quantity AS long)), 0L) AS salesVolume, " +
                    "COALESCE(SUM(s.totalAmount), 0) AS revenue " +
                    "FROM SalesTransaction s " +
                    "JOIN Product p ON s.sku = p.sku " +
                    "JOIN Brand b ON p.brandId = b.id " +
                    "WHERE s.transactionTime BETWEEN :from AND :to " +
                    "GROUP BY b.name ORDER BY revenue DESC";

            case SKU -> "SELECT s.sku AS name, " +
                    "COALESCE(SUM(CAST(s.quantity AS long)), 0L) AS salesVolume, " +
                    "COALESCE(SUM(s.totalAmount), 0) AS revenue " +
                    "FROM SalesTransaction s " +
                    "WHERE s.transactionTime BETWEEN :from AND :to " +
                    "GROUP BY s.sku ORDER BY revenue DESC";
        };
    }
}
