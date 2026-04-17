package com.retaildashboard.service;

import com.retaildashboard.domain.Inventory;
import com.retaildashboard.domain.InventoryValuationMethod;
import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.dto.DateRange;
import com.retaildashboard.dto.FilterCriteria;
import com.retaildashboard.repository.InventoryRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * P&L 핵심 계산 서비스.
 * 매출, 순매출, 매출원가, 매출총이익, EBITDA, 영업이익, 순이익을 계산합니다.
 *
 * 모든 금액 계산은 BigDecimal을 사용하여 정밀도를 보장합니다.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.8, 4.9, 4.10
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DataAggregationService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 영업비용 비율 (매출총이익 대비).
     * 실제 운영에서는 별도 테이블에서 관리하지만, 현재는 간소화된 비율을 사용합니다.
     */
    private static final BigDecimal OPERATING_EXPENSE_RATE = new BigDecimal("0.30");

    /** 감가상각비 비율 (EBITDA 대비) */
    private static final BigDecimal DEPRECIATION_RATE = new BigDecimal("0.05");

    /** 이자 비율 (영업이익 대비) */
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.03");

    /** 세율 */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.22");

    /**
     * 전체 매출 합계를 계산합니다.
     * Requirement 4.1: 모든 판매 거래의 total_amount 합계
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return 전체 매출 합계
     */
    public BigDecimal calculateTotalRevenue(DateRange range, FilterCriteria filters) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        if (filters != null && filters.getSku() != null) {
            return salesTransactionRepository.sumTotalAmountByPeriodAndSku(from, to, filters.getSku());
        }
        if (filters != null && filters.getChannel() != null) {
            return salesTransactionRepository.sumTotalAmountByPeriodAndChannel(from, to, filters.getChannel());
        }

        return salesTransactionRepository.sumTotalAmountByPeriod(from, to);
    }

    /**
     * 순매출을 계산합니다.
     * Requirement 4.2: 총매출 - 반품 - 할인
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return 순매출
     */
    public BigDecimal calculateNetRevenue(DateRange range, FilterCriteria filters) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        BigDecimal totalRevenue = calculateTotalRevenue(range, filters);
        BigDecimal returns = salesTransactionRepository.sumReturnAmountByPeriod(from, to);
        BigDecimal discounts = salesTransactionRepository.sumDiscountAmountByPeriod(from, to);

        return totalRevenue.subtract(returns).subtract(discounts);
    }

    /**
     * 매출원가(COGS)를 계산합니다.
     * Requirement 4.3: FIFO/LIFO/가중평균 재고 평가법 지원
     *
     * 간소화된 접근: 판매된 수량 × 재고 단가를 기반으로 계산합니다.
     * - FIFO: 가장 오래된 재고 단가 적용 (현재 단가의 95%)
     * - LIFO: 가장 최근 재고 단가 적용 (현재 단가의 105%)
     * - WEIGHTED_AVG: 현재 재고 단가 그대로 적용
     *
     * @param range  조회 기간
     * @param method 재고 평가 방법
     * @return 매출원가
     */
    public BigDecimal calculateCOGS(DateRange range, InventoryValuationMethod method) {
        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);

        List<String> skus = salesTransactionRepository.findDistinctSkusByPeriod(from, to);
        BigDecimal totalCogs = BigDecimal.ZERO;

        for (String sku : skus) {
            Long quantitySold = salesTransactionRepository.sumQuantityByPeriodAndSku(from, to, sku);
            if (quantitySold == null || quantitySold == 0) {
                continue;
            }

            Inventory inventory = inventoryRepository.findBySku(sku).orElse(null);
            if (inventory == null) {
                continue;
            }

            BigDecimal unitCost = inventory.getUnitCost();
            BigDecimal adjustedCost = applyValuationMethod(unitCost, method);
            BigDecimal skuCogs = adjustedCost.multiply(BigDecimal.valueOf(quantitySold));
            totalCogs = totalCogs.add(skuCogs);
        }

        return totalCogs.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 매출총이익을 계산합니다.
     * Requirement 4.4: 순매출 - 매출원가
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return 매출총이익
     */
    public BigDecimal calculateGrossProfit(DateRange range, FilterCriteria filters) {
        BigDecimal netRevenue = calculateNetRevenue(range, filters);
        BigDecimal cogs = calculateCOGS(range, InventoryValuationMethod.WEIGHTED_AVG);
        return netRevenue.subtract(cogs);
    }

    /**
     * EBITDA를 계산합니다.
     * Requirement 4.8: 매출총이익 - 영업비용(이자/세금/감가상각 제외)
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return EBITDA
     */
    public BigDecimal calculateEBITDA(DateRange range, FilterCriteria filters) {
        BigDecimal grossProfit = calculateGrossProfit(range, filters);
        BigDecimal operatingExpenses = grossProfit.multiply(OPERATING_EXPENSE_RATE);
        return grossProfit.subtract(operatingExpenses).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 영업이익을 계산합니다.
     * Requirement 4.9: EBITDA - 감가상각비
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return 영업이익
     */
    public BigDecimal calculateOperatingProfit(DateRange range, FilterCriteria filters) {
        BigDecimal ebitda = calculateEBITDA(range, filters);
        BigDecimal depreciation = ebitda.multiply(DEPRECIATION_RATE);
        return ebitda.subtract(depreciation).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 순이익을 계산합니다.
     * Requirement 4.10: 영업이익 - 이자 - 세금
     *
     * @param range   조회 기간
     * @param filters 필터 조건
     * @return 순이익
     */
    public BigDecimal calculateNetProfit(DateRange range, FilterCriteria filters) {
        BigDecimal operatingProfit = calculateOperatingProfit(range, filters);
        BigDecimal interest = operatingProfit.multiply(INTEREST_RATE);
        BigDecimal preTaxProfit = operatingProfit.subtract(interest);
        BigDecimal tax = preTaxProfit.multiply(TAX_RATE);
        return preTaxProfit.subtract(tax).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 재고 평가 방법에 따라 단가를 조정합니다.
     *
     * @param unitCost 기본 단가
     * @param method   재고 평가 방법
     * @return 조정된 단가
     */
    private BigDecimal applyValuationMethod(BigDecimal unitCost, InventoryValuationMethod method) {
        return switch (method) {
            case FIFO -> unitCost.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
            case LIFO -> unitCost.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP);
            case WEIGHTED_AVG -> unitCost;
        };
    }
}
