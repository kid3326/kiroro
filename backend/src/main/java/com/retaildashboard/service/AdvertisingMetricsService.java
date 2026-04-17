package com.retaildashboard.service;

import com.retaildashboard.domain.AdChannel;
import com.retaildashboard.domain.BudgetTarget;
import com.retaildashboard.domain.MetricType;
import com.retaildashboard.dto.ComparisonResult;
import com.retaildashboard.dto.DateRange;
import com.retaildashboard.repository.AdvertisingCostRepository;
import com.retaildashboard.repository.BudgetTargetRepository;
import com.retaildashboard.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 광고 메트릭 및 비교 분석 서비스.
 * 채널별 광고비 집계, ROAS, CAC, YoY/MoM 비교, 예산 대비 실적 차이를 계산합니다.
 *
 * Requirements: 4.5, 4.6, 4.7, 4.11, 4.12, 4.13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvertisingMetricsService {

    private final AdvertisingCostRepository advertisingCostRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final DataAggregationService dataAggregationService;

    /**
     * 채널별 광고비를 집계합니다.
     * Requirement 4.5: 채널별(Naver, Google, Meta, Others) 광고비 분류
     *
     * @param range   조회 기간
     * @param channel 광고 채널
     * @return 해당 채널의 광고비 합계
     */
    public BigDecimal calculateAdSpendByChannel(DateRange range, AdChannel channel) {
        return advertisingCostRepository.sumSpendAmountByPeriodAndChannel(
                range.startDate(), range.endDate(), channel.name());
    }

    /**
     * 전체 광고비를 집계합니다.
     *
     * @param range 조회 기간
     * @return 전체 광고비 합계
     */
    public BigDecimal calculateTotalAdSpend(DateRange range) {
        return advertisingCostRepository.sumSpendAmountByPeriod(range.startDate(), range.endDate());
    }

    /**
     * 채널별 ROAS를 계산합니다.
     * Requirement 4.6: 채널별 매출 / 광고비
     *
     * @param range   조회 기간
     * @param channel 광고 채널
     * @return ROAS (광고비가 0이면 ZERO 반환)
     */
    public BigDecimal calculateROAS(DateRange range, AdChannel channel) {
        BigDecimal adSpend = calculateAdSpendByChannel(range, channel);
        if (adSpend.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        LocalDateTime from = range.startDate().atStartOfDay();
        LocalDateTime to = range.endDate().atTime(LocalTime.MAX);
        BigDecimal channelRevenue = salesTransactionRepository.sumTotalAmountByPeriodAndChannel(
                from, to, channel.name());

        return channelRevenue.divide(adSpend, 4, RoundingMode.HALF_UP);
    }

    /**
     * CAC(고객 획득 비용)를 계산합니다.
     * Requirement 4.7: 총 광고비 / 신규 고객 수
     *
     * @param range 조회 기간
     * @return CAC (신규 고객이 0이면 ZERO 반환)
     */
    public BigDecimal calculateCAC(DateRange range) {
        BigDecimal totalAdSpend = calculateTotalAdSpend(range);
        Long newCustomers = advertisingCostRepository.sumNewCustomersByPeriod(
                range.startDate(), range.endDate());

        if (newCustomers == null || newCustomers == 0) {
            return BigDecimal.ZERO;
        }

        return totalAdSpend.divide(BigDecimal.valueOf(newCustomers), 2, RoundingMode.HALF_UP);
    }

    /**
     * YoY(전년 동기 대비) 비교를 계산합니다.
     * Requirement 4.11: 현재 기간 값 - 전년 동기 값
     *
     * @param range  현재 조회 기간
     * @param metric 비교할 메트릭 유형
     * @return 비교 결과
     */
    public ComparisonResult calculateYoY(DateRange range, MetricType metric) {
        DateRange previousYearRange = new DateRange(
                range.startDate().minusYears(1),
                range.endDate().minusYears(1)
        );

        BigDecimal currentValue = getMetricValue(range, metric);
        BigDecimal previousValue = getMetricValue(previousYearRange, metric);

        return buildComparisonResult(currentValue, previousValue);
    }

    /**
     * MoM(전월 대비) 비교를 계산합니다.
     * Requirement 4.12: 현재 월 값 - 전월 값
     *
     * @param range  현재 조회 기간
     * @param metric 비교할 메트릭 유형
     * @return 비교 결과
     */
    public ComparisonResult calculateMoM(DateRange range, MetricType metric) {
        long daysBetween = ChronoUnit.DAYS.between(range.startDate(), range.endDate());
        DateRange previousMonthRange = new DateRange(
                range.startDate().minusMonths(1),
                range.startDate().minusMonths(1).plusDays(daysBetween)
        );

        BigDecimal currentValue = getMetricValue(range, metric);
        BigDecimal previousValue = getMetricValue(previousMonthRange, metric);

        return buildComparisonResult(currentValue, previousValue);
    }

    /**
     * 예산 대비 실적 차이를 계산합니다.
     * Requirement 4.13: 실적 - 예산
     *
     * @param range  조회 기간
     * @param metric 비교할 메트릭 유형
     * @return 비교 결과 (previousValue에 예산 값이 들어감)
     */
    public ComparisonResult calculateBudgetVariance(DateRange range, MetricType metric) {
        BigDecimal actualValue = getMetricValue(range, metric);

        LocalDate targetMonth = range.startDate().withDayOfMonth(1);
        String budgetMetricType = mapToBudgetMetricType(metric);

        BigDecimal budgetValue = budgetTargetRepository
                .findByTargetMonthAndMetricType(targetMonth, budgetMetricType)
                .map(BudgetTarget::getTargetValue)
                .orElse(BigDecimal.ZERO);

        return buildComparisonResult(actualValue, budgetValue);
    }

    /**
     * 메트릭 유형에 따라 해당 기간의 값을 조회합니다.
     */
    private BigDecimal getMetricValue(DateRange range, MetricType metric) {
        return switch (metric) {
            case TOTAL_REVENUE -> dataAggregationService.calculateTotalRevenue(range, null);
            case NET_REVENUE -> dataAggregationService.calculateNetRevenue(range, null);
            case GROSS_PROFIT -> dataAggregationService.calculateGrossProfit(range, null);
            case EBITDA -> dataAggregationService.calculateEBITDA(range, null);
            case OPERATING_PROFIT -> dataAggregationService.calculateOperatingProfit(range, null);
            case NET_PROFIT -> dataAggregationService.calculateNetProfit(range, null);
            case AD_SPEND -> calculateTotalAdSpend(range);
        };
    }

    /**
     * 비교 결과를 생성합니다.
     */
    private ComparisonResult buildComparisonResult(BigDecimal currentValue, BigDecimal previousValue) {
        BigDecimal difference = currentValue.subtract(previousValue);
        BigDecimal changeRate = BigDecimal.ZERO;

        if (previousValue.compareTo(BigDecimal.ZERO) != 0) {
            changeRate = difference
                    .divide(previousValue.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ComparisonResult(currentValue, previousValue, difference, changeRate);
    }

    /**
     * MetricType을 budget_metric_type DB enum 값으로 매핑합니다.
     */
    private String mapToBudgetMetricType(MetricType metric) {
        return switch (metric) {
            case TOTAL_REVENUE, NET_REVENUE -> "REVENUE";
            case AD_SPEND -> "AD_SPEND";
            case GROSS_PROFIT, EBITDA, OPERATING_PROFIT, NET_PROFIT -> "PROFIT";
        };
    }
}
