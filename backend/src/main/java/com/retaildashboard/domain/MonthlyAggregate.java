package com.retaildashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월별 집계 엔티티.
 * monthly_aggregates 테이블과 매핑됩니다.
 *
 * Requirements: 8.5, 14.2
 */
@Entity
@Table(name = "monthly_aggregates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_month", nullable = false)
    private LocalDate aggregateMonth;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "brand", length = 200)
    private String brand;

    @Column(name = "total_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "net_revenue", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netRevenue = BigDecimal.ZERO;

    @Column(name = "cogs", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cogs = BigDecimal.ZERO;

    @Column(name = "gross_profit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grossProfit = BigDecimal.ZERO;

    @Column(name = "ebitda", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ebitda = BigDecimal.ZERO;

    @Column(name = "operating_profit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal operatingProfit = BigDecimal.ZERO;

    @Column(name = "net_profit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;

    @Column(name = "total_ad_spend", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAdSpend = BigDecimal.ZERO;

    @Column(name = "avg_roas", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal avgRoas = BigDecimal.ZERO;

    @Column(name = "cac", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cac = BigDecimal.ZERO;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
