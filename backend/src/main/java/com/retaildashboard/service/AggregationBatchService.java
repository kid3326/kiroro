package com.retaildashboard.service;

import com.retaildashboard.domain.AdChannel;
import com.retaildashboard.domain.DailyAggregate;
import com.retaildashboard.domain.InventoryValuationMethod;
import com.retaildashboard.domain.MonthlyAggregate;
import com.retaildashboard.domain.SalesTransaction;
import com.retaildashboard.dto.DateRange;
import com.retaildashboard.dto.FilterCriteria;
import com.retaildashboard.repository.AdvertisingCostRepository;
import com.retaildashboard.repository.DailyAggregateRepository;
import com.retaildashboard.repository.MonthlyAggregateRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사전 집계 배치 처리 서비스.
 * 일별/월별 집계를 수행하고 Redis 캐시를 갱신합니다.
 *
 * @Async를 사용하여 스케줄러를 블로킹하지 않습니다.
 *
 * Requirements: 8.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AggregationBatchService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final AdvertisingCostRepository advertisingCostRepository;
    private final DailyAggregateRepository dailyAggregateRepository;
    private final MonthlyAggregateRepository monthlyAggregateRepository;
    private final DataAggregationService dataAggregationService;
    private final AdvertisingMetricsService advertisingMetricsService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "dashboard:aggregate:";
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    /**
     * 일별 집계 배치를 실행합니다.
     * 지정된 날짜의 판매 데이터를 SKU별로 집계하여 daily_aggregates 테이블에 저장합니다.
     *
     * @param date 집계 대상 날짜
     */
    @Async
    @Transactional
    public void runDailyAggregation(LocalDate date) {
        log.info("일별 집계 시작: {}", date);

        try {
            // 기존 집계 데이터 삭제 (재집계 지원)
            dailyAggregateRepository.deleteByAggregateDate(date);

            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.atTime(LocalTime.MAX);

            List<SalesTransaction> transactions = salesTransactionRepository
                    .findByTransactionTimeBetween(from, to);

            if (transactions.isEmpty()) {
                log.info("일별 집계 완료: {} - 거래 데이터 없음", date);
                return;
            }

            // SKU별 집계
            Map<String, SkuAggregation> skuAggregations = new HashMap<>();
            for (SalesTransaction tx : transactions) {
                skuAggregations.computeIfAbsent(tx.getSku(), k -> new SkuAggregation())
                        .add(tx);
            }

            DateRange dayRange = new DateRange(date, date);
            BigDecimal totalAdSpend = advertisingMetricsService.calculateTotalAdSpend(dayRange);

            List<DailyAggregate> aggregates = new ArrayList<>();
            for (Map.Entry<String, SkuAggregation> entry : skuAggregations.entrySet()) {
                String sku = entry.getKey();
                SkuAggregation agg = entry.getValue();

                BigDecimal totalRevenue = agg.totalAmount;
                BigDecimal netRevenue = totalRevenue.subtract(agg.returnAmount).subtract(agg.discountAmount);

                // SKU별 COGS 간소화 계산
                BigDecimal cogs = dataAggregationService.calculateCOGS(dayRange, InventoryValuationMethod.WEIGHTED_AVG);
                BigDecimal grossProfit = netRevenue.subtract(cogs);

                // SKU별 광고비 비례 배분 (매출 비율 기준)
                BigDecimal revenueRatio = BigDecimal.ZERO;
                BigDecimal totalDayRevenue = dataAggregationService.calculateTotalRevenue(dayRange, null);
                if (totalDayRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    revenueRatio = totalRevenue.divide(totalDayRevenue, 6, RoundingMode.HALF_UP);
                }
                BigDecimal skuAdSpend = totalAdSpend.multiply(revenueRatio).setScale(2, RoundingMode.HALF_UP);

                BigDecimal roas = BigDecimal.ZERO;
                if (skuAdSpend.compareTo(BigDecimal.ZERO) > 0) {
                    roas = totalRevenue.divide(skuAdSpend, 4, RoundingMode.HALF_UP);
                }

                DailyAggregate aggregate = DailyAggregate.builder()
                        .aggregateDate(date)
                        .sku(sku)
                        .totalRevenue(totalRevenue)
                        .netRevenue(netRevenue)
                        .cogs(cogs)
                        .grossProfit(grossProfit)
                        .salesVolume(agg.quantity)
                        .adSpend(skuAdSpend)
                        .roas(roas)
                        .calculatedAt(LocalDateTime.now())
                        .build();

                aggregates.add(aggregate);
            }

            dailyAggregateRepository.saveAll(aggregates);
            log.info("일별 집계 완료: {} - {}건 저장", date, aggregates.size());

            // Redis 캐시 갱신
            updateDailyCache(date);

        } catch (Exception e) {
            log.error("일별 집계 실패: {} - {}", date, e.getMessage(), e);
        }
    }

    /**
     * 월별 집계 배치를 실행합니다.
     * 지정된 월의 일별 집계 데이터를 합산하여 monthly_aggregates 테이블에 저장합니다.
     *
     * @param month 집계 대상 월 (해당 월의 1일)
     */
    @Async
    @Transactional
    public void runMonthlyAggregation(LocalDate month) {
        LocalDate firstDay = month.withDayOfMonth(1);
        LocalDate lastDay = month.withDayOfMonth(month.lengthOfMonth());

        log.info("월별 집계 시작: {} ~ {}", firstDay, lastDay);

        try {
            // 기존 월별 집계 삭제
            monthlyAggregateRepository.deleteByAggregateMonth(firstDay);

            DateRange monthRange = new DateRange(firstDay, lastDay);

            BigDecimal totalRevenue = dataAggregationService.calculateTotalRevenue(monthRange, null);
            BigDecimal netRevenue = dataAggregationService.calculateNetRevenue(monthRange, null);
            BigDecimal cogs = dataAggregationService.calculateCOGS(monthRange, InventoryValuationMethod.WEIGHTED_AVG);
            BigDecimal grossProfit = dataAggregationService.calculateGrossProfit(monthRange, null);
            BigDecimal ebitda = dataAggregationService.calculateEBITDA(monthRange, null);
            BigDecimal operatingProfit = dataAggregationService.calculateOperatingProfit(monthRange, null);
            BigDecimal netProfit = dataAggregationService.calculateNetProfit(monthRange, null);
            BigDecimal totalAdSpend = advertisingMetricsService.calculateTotalAdSpend(monthRange);
            BigDecimal cac = advertisingMetricsService.calculateCAC(monthRange);

            // 평균 ROAS 계산 (전체 채널)
            BigDecimal avgRoas = BigDecimal.ZERO;
            if (totalAdSpend.compareTo(BigDecimal.ZERO) > 0) {
                avgRoas = totalRevenue.divide(totalAdSpend, 4, RoundingMode.HALF_UP);
            }

            MonthlyAggregate aggregate = MonthlyAggregate.builder()
                    .aggregateMonth(firstDay)
                    .totalRevenue(totalRevenue)
                    .netRevenue(netRevenue)
                    .cogs(cogs)
                    .grossProfit(grossProfit)
                    .ebitda(ebitda)
                    .operatingProfit(operatingProfit)
                    .netProfit(netProfit)
                    .totalAdSpend(totalAdSpend)
                    .avgRoas(avgRoas)
                    .cac(cac)
                    .calculatedAt(LocalDateTime.now())
                    .build();

            monthlyAggregateRepository.save(aggregate);
            log.info("월별 집계 완료: {}", firstDay);

            // Redis 캐시 갱신
            updateMonthlyCache(firstDay);

        } catch (Exception e) {
            log.error("월별 집계 실패: {} - {}", firstDay, e.getMessage(), e);
        }
    }

    /**
     * 데이터 수집 완료 후 자동 집계를 트리거합니다.
     * 오늘 날짜의 일별 집계와 현재 월의 월별 집계를 수행합니다.
     */
    @Async
    public void triggerAggregation() {
        LocalDate today = LocalDate.now();
        log.info("자동 집계 트리거: {}", today);

        runDailyAggregation(today);
        runMonthlyAggregation(today);
    }

    /**
     * 일별 캐시를 갱신합니다.
     */
    private void updateDailyCache(LocalDate date) {
        try {
            List<DailyAggregate> aggregates = dailyAggregateRepository
                    .findByAggregateDateBetween(date, date);
            String cacheKey = CACHE_PREFIX + "daily:" + date;
            redisTemplate.opsForValue().set(cacheKey, aggregates, CACHE_TTL);
            log.debug("일별 캐시 갱신 완료: {}", cacheKey);
        } catch (Exception e) {
            log.warn("일별 캐시 갱신 실패: {} - {}", date, e.getMessage());
        }
    }

    /**
     * 월별 캐시를 갱신합니다.
     */
    private void updateMonthlyCache(LocalDate month) {
        try {
            List<MonthlyAggregate> aggregates = monthlyAggregateRepository
                    .findByAggregateMonth(month);
            String cacheKey = CACHE_PREFIX + "monthly:" + month;
            redisTemplate.opsForValue().set(cacheKey, aggregates, CACHE_TTL);
            log.debug("월별 캐시 갱신 완료: {}", cacheKey);
        } catch (Exception e) {
            log.warn("월별 캐시 갱신 실패: {} - {}", month, e.getMessage());
        }
    }

    /**
     * SKU별 집계를 위한 내부 헬퍼 클래스.
     */
    private static class SkuAggregation {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal returnAmount = BigDecimal.ZERO;
        int quantity = 0;

        void add(SalesTransaction tx) {
            totalAmount = totalAmount.add(tx.getTotalAmount());
            discountAmount = discountAmount.add(tx.getDiscountAmount());
            returnAmount = returnAmount.add(tx.getReturnAmount());
            quantity += tx.getQuantity();
        }
    }
}
